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
        boolean isAll = normalized.isEmpty() || "ALL".equals(normalized);

        List<User> users = isAll
                ? userRepository.findByIdentityReviewStatusIsNotNullOrderByIdentityConfirmedAtDesc()
                : userRepository.findByIdentityReviewStatusOrderByIdentityConfirmedAtDesc(normalized);

        List<Reservation> ticketReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED)
                .filter(r -> {
                    String effectiveStatus = r.getReviewStatus();
                    if (effectiveStatus == null && hasAnyTicketDocument(r)) {
                        effectiveStatus = "PENDING_REVIEW";
                    }
                    if (effectiveStatus == null) {
                        return false;
                    }
                    return isAll || normalized.equals(effectiveStatus);
                })
                .toList();

        Map<String, List<Reservation>> groupedTickets = new LinkedHashMap<>();
        for (Reservation r : ticketReservations) {
            String code = r.getVoucherCode();
            if (code == null || code.isBlank()) {
                code = "RES-" + r.getId();
            }
            groupedTickets.computeIfAbsent(code, k -> new ArrayList<>()).add(r);
        }

        List<IdentityVerificationAdminDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Reservation>> entry : groupedTickets.entrySet()) {
            result.add(toTicketReviewDto(entry.getKey(), entry.getValue()));
        }
        for (User user : users) {
            result.add(toDto(user));
        }

        result.sort(Comparator.comparing(
                (IdentityVerificationAdminDto dto) -> {
                    if (dto.getTicket() != null && dto.getTicket().getPurchaseDate() != null) {
                        return dto.getTicket().getPurchaseDate();
                    }
                    return dto.getIdentityConfirmedAt();
                },
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    @Override
    @Transactional
    public IdentityVerificationAdminDto approveTicket(String ticketCode, String reviewerUsername) {
        List<Reservation> group = reservationRepository.findByVoucherCodeIgnoreCase(ticketCode);
        if (group.isEmpty()) {
            throw new ResourceNotFoundException("Bilet tapılmadı: " + ticketCode);
        }
        LocalDateTime now = LocalDateTime.now();
        for (Reservation r : group) {
            r.setReviewStatus("APPROVED");
            r.setReviewedAt(now);
            r.setReviewedBy(reviewerUsername);
            r.setReviewReason(null);
            if (r.getUser() != null) {
                User u = r.getUser();
                u.setIdentityReviewStatus("APPROVED");
                u.setIdentityConfirmedAt(now);
                u.setAgeVerifiedAt(now);
                u.setIdentityReviewedAt(now);
                u.setIdentityReviewedBy(reviewerUsername);
                u.setIdentityRejectionReason(null);
                userRepository.save(u);
            }
        }
        reservationRepository.saveAll(group);
        return toTicketReviewDto(ticketCode, group);
    }

    @Override
    @Transactional
    public IdentityVerificationAdminDto rejectTicket(String ticketCode, String reason, String reviewerUsername) {
        List<Reservation> group = reservationRepository.findByVoucherCodeIgnoreCase(ticketCode);
        if (group.isEmpty()) {
            throw new ResourceNotFoundException("Bilet tapılmadı: " + ticketCode);
        }
        String cleanedReason = reason == null || reason.isBlank()
                ? "Təqdim olunan sənəd və ya foto məlumatları qaydalara uyğun deyil."
                : reason.trim();
        LocalDateTime now = LocalDateTime.now();
        Set<Long> cancelledIds = new HashSet<>();
        Long userId = null;

        for (Reservation reservation : group) {
            if (reservation.getUser() != null) {
                userId = reservation.getUser().getId();
            }
            reservation.setReviewStatus("REJECTED");
            reservation.setReviewedAt(now);
            reservation.setReviewedBy(reviewerUsername);
            reservation.setReviewReason(cleanedReason);

            if (reservation.getCheckedInAt() == null
                    && reservation.getStatus() != Reservation.Status.CANCELLED) {
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
                if (reservation.getId() != null) {
                    cancelledIds.add(reservation.getId());
                }
            }
            reservationRepository.save(reservation);
        }

        BigDecimal refunded = BigDecimal.ZERO;
        if (!cancelledIds.isEmpty() && userId != null) {
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

        IdentityVerificationAdminDto dto = toTicketReviewDto(ticketCode, group);
        dto.setRefundedAmount(refunded.compareTo(BigDecimal.ZERO) > 0 ? refunded : null);
        return dto;
    }

    @Override
    @Transactional
    public IdentityVerificationAdminDto approve(Long userId, String reviewerUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + userId));
        LocalDateTime now = LocalDateTime.now();
        user.setIdentityReviewStatus("APPROVED");
        user.setIdentityConfirmedAt(now);
        user.setAgeVerifiedAt(now);
        user.setIdentityReviewedAt(now);
        user.setIdentityReviewedBy(reviewerUsername);
        user.setIdentityRejectionReason(null);
        userRepository.save(user);

        List<Reservation> userReservations = reservationRepository.findByUserId(userId);
        for (Reservation reservation : userReservations) {
            if ("PENDING_REVIEW".equals(reservation.getReviewStatus())) {
                reservation.setReviewStatus("APPROVED");
                reservation.setReviewedAt(now);
                reservation.setReviewedBy(reviewerUsername);
                reservation.setReviewReason(null);
            }
        }
        reservationRepository.saveAll(userReservations);

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
                .itemType("USER_IDENTITY")
                .reviewStatus(user.getIdentityReviewStatus())
                .reviewReason(user.getIdentityRejectionReason())
                .reviewedAt(user.getIdentityReviewedAt())
                .reviewedBy(user.getIdentityReviewedBy())
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
                .familyIdsPhotoData(first.getFamilyIdsPhotoData())
                .push30ScreenshotData(first.getPush30ScreenshotData())
                .push30Phone(first.getPush30Phone())
                .qaziDocumentPhotoData(first.getQaziDocumentPhotoData())
                .qaziFullName(first.getQaziFullName())
                .shaheedDocumentPhotoData(first.getShaheedDocumentPhotoData())
                .participantPhotos(participantPhotos)
                .build();
    }

    private IdentityVerificationAdminDto toTicketReviewDto(String code, List<Reservation> group) {
        Reservation first = group.get(0);
        User user = first.getUser();
        String firstName = user != null && user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user != null && user.getLastName() != null ? user.getLastName().trim() : "";
        String accountFullName = (firstName + " " + lastName).trim();
        if (accountFullName.isEmpty() && user != null) {
            accountFullName = user.getUsername();
        }

        List<String> participantNames = group.stream()
                .map(Reservation::getParticipantName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        IdentityVerificationAdminDto.TicketSummary summary = toTicketSummary(code, group);
        String effectiveStatus = first.getReviewStatus();
        if (effectiveStatus == null && group.stream().anyMatch(this::hasAnyTicketDocument)) {
            effectiveStatus = "PENDING_REVIEW";
        }
        return IdentityVerificationAdminDto.builder()
                .userId(user != null ? user.getId() : null)
                .itemType("TICKET")
                .ticketCode(code)
                .discountType(first.getDiscountType())
                .reviewStatus(effectiveStatus)
                .reviewReason(first.getReviewReason())
                .reviewedAt(first.getReviewedAt())
                .reviewedBy(first.getReviewedBy())
                .participantNames(participantNames)
                .ticket(summary)
                .username(user != null ? user.getUsername() : null)
                .fullName(accountFullName)
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .dateOfBirth(user != null ? user.getDateOfBirth() : null)
                .identityDocumentNumber(user != null ? user.getIdentityDocumentNumber() : null)
                .finCode(user != null ? user.getFinCode() : null)
                .identityReviewStatus(user != null ? user.getIdentityReviewStatus() : null)
                .identityConfirmedAt(user != null ? user.getIdentityConfirmedAt() : null)
                .build();
    }

    private boolean hasAnyTicketDocument(Reservation r) {
        return (r.getFamilyIdsPhotoData() != null && !r.getFamilyIdsPhotoData().isBlank())
                || (r.getPush30ScreenshotData() != null && !r.getPush30ScreenshotData().isBlank())
                || (r.getQaziDocumentPhotoData() != null && !r.getQaziDocumentPhotoData().isBlank())
                || (r.getShaheedDocumentPhotoData() != null && !r.getShaheedDocumentPhotoData().isBlank())
                || (r.getStudentCardPhotoData() != null && !r.getStudentCardPhotoData().isBlank())
                || (r.getIdentityPhotoData() != null && !r.getIdentityPhotoData().isBlank());
    }
}
