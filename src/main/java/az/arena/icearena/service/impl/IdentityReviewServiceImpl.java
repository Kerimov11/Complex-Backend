package az.arena.icearena.service.impl;

import az.arena.icearena.dto.admin.IdentityVerificationAdminDto;
import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.PaymentStatus;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.PaymentRepository;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.IdentityReviewService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityReviewServiceImpl implements IdentityReviewService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final SessionRepository sessionRepository;
    private final SkateRepository skateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IdentityVerificationAdminDto> list(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        List<User> users = normalized.isEmpty() || "ALL".equals(normalized)
                ? userRepository.findByIdentityReviewStatusIsNotNullOrderByIdentityConfirmedAtDesc()
                : userRepository.findByIdentityReviewStatusOrderByIdentityConfirmedAtDesc(normalized);
        return users.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public IdentityVerificationAdminDto approve(Long userId, String reviewerUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + userId));
        user.setIdentityReviewStatus("APPROVED");
        user.setIdentityReviewedAt(LocalDateTime.now());
        user.setIdentityReviewedBy(reviewerUsername);
        user.setIdentityRejectionReason(null);
        userRepository.save(user);
        return toDto(user);
    }

    @Override
    @Transactional
    public IdentityVerificationAdminDto reject(Long userId, String reason, String reviewerUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + userId));
        String cleanedReason = reason == null || reason.isBlank()
                ? "Şəkil və ya sənəd məlumatları qaydalara uyğun deyil. Zəhmət olmasa yenidən yükləyin."
                : reason.trim();
        user.setIdentityReviewStatus("REJECTED");
        user.setIdentityReviewedAt(LocalDateTime.now());
        user.setIdentityReviewedBy(reviewerUsername);
        user.setIdentityRejectionReason(cleanedReason);
        userRepository.save(user);

        Set<Long> cancelledIds = new HashSet<>();
        List<Reservation> userReservations = reservationRepository.findByUserId(userId);
        for (Reservation reservation : userReservations) {
            if (reservation.getCheckedInAt() != null
                    || reservation.getStatus() == Reservation.Status.CANCELLED) {
                continue;
            }
            reservation.setStatus(Reservation.Status.CANCELLED);
            if (Boolean.TRUE.equals(reservation.getSkateStockDeducted())
                    && reservation.getSkateHandedOutAt() == null
                    && reservation.getSkate() != null) {
                Skate skate = reservation.getSkate();
                int avail = skate.getAvailableQuantity() == null ? 0 : skate.getAvailableQuantity();
                skate.setAvailableQuantity(avail + 1);
                skateRepository.save(skate);
                reservation.setSkateStockDeducted(false);
                reservation.setTimerExpiresAt(null);
            }
            if (reservation.getSession() != null) {
                Session session = reservation.getSession();
                int max = session.getMaxTicketLimit() == null ? Integer.MAX_VALUE : session.getMaxTicketLimit();
                int avail = session.getAvailableTickets() == null ? 0 : session.getAvailableTickets();
                session.setAvailableTickets(Math.min(max, avail + 1));
                sessionRepository.save(session);
            }
            reservationRepository.save(reservation);
            if (reservation.getId() != null) {
                cancelledIds.add(reservation.getId());
            }
        }

        BigDecimal refunded = BigDecimal.ZERO;
        if (!cancelledIds.isEmpty()) {
            List<Payment> payments = paymentRepository.findByUserId(userId);
            for (Payment payment : payments) {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    continue;
                }
                if (matchesAnyReservation(payment.getReservationIds(), cancelledIds)) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                    if (payment.getAmount() != null) {
                        refunded = refunded.add(payment.getAmount());
                    }
                }
            }
        }

        IdentityVerificationAdminDto dto = toDto(user);
        dto.setRefundedAmount(refunded.compareTo(BigDecimal.ZERO) > 0 ? refunded : null);
        return dto;
    }

    private boolean matchesAnyReservation(String reservationIdsCsv, Set<Long> targetIds) {
        if (reservationIdsCsv == null || reservationIdsCsv.isBlank()) {
            return false;
        }
        return Arrays.stream(reservationIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(s -> {
                    try {
                        return targetIds.contains(Long.parseLong(s));
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                });
    }

    private IdentityVerificationAdminDto toDto(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (first + " " + last).trim();
        if (fullName.isEmpty()) {
            fullName = user.getUsername();
        }

        List<Reservation> reservations = reservationRepository.findByUserId(user.getId());
        Map<String, List<Reservation>> grouped = new LinkedHashMap<>();
        for (Reservation r : reservations) {
            String code = r.getVoucherCode();
            if (code == null || code.isBlank()) {
                code = "RES-" + r.getId();
            }
            grouped.computeIfAbsent(code, k -> new ArrayList<>()).add(r);
        }

        List<IdentityVerificationAdminDto.TicketSummary> tickets = grouped.entrySet().stream()
                .map(entry -> toTicketSummary(entry.getKey(), entry.getValue()))
                .toList();

        return IdentityVerificationAdminDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .identityDocumentNumber(user.getIdentityDocumentNumber())
                .finCode(user.getFinCode())
                .identityPhotoData(user.getIdentityPhotoData())
                .identityReviewStatus(user.getIdentityReviewStatus())
                .identityConfirmedAt(user.getIdentityConfirmedAt())
                .identityReviewedAt(user.getIdentityReviewedAt())
                .identityReviewedBy(user.getIdentityReviewedBy())
                .identityRejectionReason(user.getIdentityRejectionReason())
                .tickets(tickets)
                .build();
    }

    private IdentityVerificationAdminDto.TicketSummary toTicketSummary(String code, List<Reservation> group) {
        Reservation first = group.get(0);
        List<String> sizes = group.stream()
                .map(Reservation::getSkate)
                .filter(s -> s != null && s.getSize() != null)
                .sorted(Comparator.comparing(Skate::getSize))
                .map(Skate::getDisplaySize)
                .toList();
        LocalDateTime purchaseDate = group.stream()
                .map(Reservation::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        List<IdentityVerificationAdminDto.ParticipantPhotoSummary> participantPhotos = group.stream()
                .map(r -> IdentityVerificationAdminDto.ParticipantPhotoSummary.builder()
                        .reservationId(r.getId())
                        .participantName(r.getParticipantName())
                        .participantAge(r.getParticipantAge())
                        .skateSize(r.getSkate() != null ? r.getSkate().getDisplaySize() : null)
                        .identityPhotoData(r.getIdentityPhotoData())
                        .studentCardPhotoData(r.getStudentCardPhotoData())
                        .studentDiscountApplied(Boolean.TRUE.equals(r.getStudentDiscountApplied()))
                        .build())
                .toList();

        return IdentityVerificationAdminDto.TicketSummary.builder()
                .ticketCode(code)
                .sessionDate(first.getSession() != null ? first.getSession().getSessionDate() : null)
                .durationMinutes(first.getDurationMinutes())
                .participantCount(group.size())
                .skateSizes(sizes)
                .status(first.getStatus() != null ? first.getStatus().name() : null)
                .purchaseDate(purchaseDate)
                .participantPhotos(participantPhotos)
                .build();
    }
}
