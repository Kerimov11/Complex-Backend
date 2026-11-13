package az.arena.icearena.service.impl;

import az.arena.icearena.dto.admin.IdentityVerificationAdminDto;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.IdentityReviewService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityReviewServiceImpl implements IdentityReviewService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

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
        return toDto(user);
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

        return IdentityVerificationAdminDto.TicketSummary.builder()
                .ticketCode(code)
                .sessionDate(first.getSession() != null ? first.getSession().getSessionDate() : null)
                .durationMinutes(first.getDurationMinutes())
                .participantCount(group.size())
                .skateSizes(sizes)
                .status(first.getStatus() != null ? first.getStatus().name() : null)
                .purchaseDate(purchaseDate)
                .build();
    }
}
