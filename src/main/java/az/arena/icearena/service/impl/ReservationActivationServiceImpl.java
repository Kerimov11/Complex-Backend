package az.arena.icearena.service.impl;

import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.service.ReservationActivationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationActivationServiceImpl implements ReservationActivationService {

    public static final int MAX_ACTIVATION_ATTEMPTS = 2;
    public static final int RESERVATION_MINUTES = 10;

    private final ReservationRepository reservationRepository;
    private final SkateRepository skateRepository;

    @Override
    @Transactional
    public List<Reservation> activateByCode(String ticketCode, String username, boolean admin) {
        expireStaleReservations();

        String code = ticketCode == null ? "" : ticketCode.trim();
        if (code.isEmpty()) {
            throw new BadRequestException("Bilet kodu tələb olunur.");
        }

        List<Reservation> reservations = reservationRepository.findByVoucherCodeIgnoreCase(code);
        if (reservations.isEmpty()) {
            reservations = findLegacyReservationGroup(code);
        }
        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("Bu bilet üçün konki rezervasiyası tapılmadı.");
        }

        reservations.forEach(reservation -> ensureOwner(reservation, username, admin));
        return activateGroup(reservations);
    }

    @Override
    @Transactional
    public List<Reservation> activateReservation(Long reservationId, String username, boolean admin) {
        expireStaleReservations();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rezervasiya tapılmadı: " + reservationId));
        ensureOwner(reservation, username, admin);

        List<Reservation> reservations = reservation.getVoucherCode() == null
                ? List.of(reservation)
                : reservationRepository.findByVoucherCodeIgnoreCase(reservation.getVoucherCode());
        return activateGroup(reservations.isEmpty() ? List.of(reservation) : reservations);
    }

    @Override
    @Scheduled(fixedDelay = 15_000)
    @Transactional
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationRepository
                .findBySkateStockDeductedTrueAndSkateHandedOutAtIsNullAndTimerExpiresAtBefore(now);
        for (Reservation reservation : expired) {
            restoreSkate(reservation);
            reservation.setSkateStockDeducted(false);
            reservation.setTimerExpiresAt(null);
            reservation.setSkateReservedAt(null);
            reservation.setSkateReservedBy(null);
            reservationRepository.save(reservation);
        }
    }

    private List<Reservation> activateGroup(List<Reservation> reservations) {
        LocalDateTime now = LocalDateTime.now();

        for (Reservation reservation : reservations) {
            String ticketReviewStatus = reservation.getReviewStatus();
            if ("PENDING_REVIEW".equals(ticketReviewStatus)) {
                throw new BadRequestException(
                        "Biletiniz hələ kassir tərəfindən təsdiqlənməyib. Təsdiqdən sonra konki rezervasiyası edə bilərsiniz.");
            }
            if ("REJECTED".equals(ticketReviewStatus)) {
                throw new BadRequestException(
                        "Bu biletin təsdiqi rədd edilib.");
            }
            if (reservation.getUser() != null) {
                String reviewStatus = reservation.getUser().getIdentityReviewStatus();
                if ("PENDING_REVIEW".equals(reviewStatus)) {
                    throw new BadRequestException(
                            "Biletiniz hələ kassir tərəfindən təsdiqlənməyib. Təsdiqdən sonra konki rezervasiyası edə bilərsiniz.");
                }
                if ("REJECTED".equals(reviewStatus)) {
                    throw new BadRequestException(
                            "Şəxsiyyət təsdiqiniz rədd edilib. Zəhmət olmasa Hesabım bölməsindən yeni foto göndərin.");
                }
            }
            if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
                throw new BadRequestException(
                        "Yalnız ödənişi təsdiqlənmiş biletlər üçün konki rezervasiyası aktivləşdirilə bilər.");
            }
            if (reservation.getCheckedInAt() != null) {
                throw new BadRequestException("Bu bilet artıq istifadə edilib.");
            }
            if (reservation.getSkateHandedOutAt() != null) {
                throw new BadRequestException("Konki artıq müştəriyə təhvil verilib.");
            }
            if (reservation.getTimerExpiresAt() != null
                    && reservation.getTimerExpiresAt().isAfter(now)) {
                throw new BadRequestException("Konki rezervasiyası artıq aktivdir.");
            }
            int attempts = reservation.getReservationExtensionCount() == null
                    ? 0
                    : reservation.getReservationExtensionCount();
            if (attempts >= MAX_ACTIVATION_ATTEMPTS) {
                throw new BadRequestException(
                        "Bu bilet üçün 2 aktivləşdirmə cəhdi artıq istifadə olunub.");
            }
        }

        Map<Long, Integer> requiredBySkate = new LinkedHashMap<>();
        Map<Long, Skate> skateById = new LinkedHashMap<>();
        for (Reservation reservation : reservations) {
            Skate skate = reservation.getSkate();
            if (skate == null || skate.getId() == null) {
                throw new BadRequestException("Rezervasiyada konki ölçüsü göstərilməyib.");
            }
            skateById.put(skate.getId(), skate);
            requiredBySkate.merge(skate.getId(), 1, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : requiredBySkate.entrySet()) {
            Skate skate = skateById.get(entry.getKey());
            int required = entry.getValue();
            int available = skate.getAvailableQuantity() == null ? 0 : skate.getAvailableQuantity();
            if (available < required) {
                throw new BadRequestException(
                        "Bu ölçüdə kifayət qədər konki yoxdur (ölçü "
                                + skate.getDisplaySize() + ", tələb " + required
                                + ", mövcud " + available + ").");
            }
        }

        for (Map.Entry<Long, Integer> entry : requiredBySkate.entrySet()) {
            Skate skate = skateById.get(entry.getKey());
            skate.setAvailableQuantity(skate.getAvailableQuantity() - entry.getValue());
            skateRepository.save(skate);
        }

        List<Reservation> activated = new ArrayList<>();
        for (Reservation reservation : reservations) {
            int attempts = reservation.getReservationExtensionCount() == null
                    ? 0
                    : reservation.getReservationExtensionCount();
            reservation.setReservationExtensionCount(attempts + 1);
            reservation.setSkateStockDeducted(true);
            reservation.setTimerExpiresAt(now.plusMinutes(RESERVATION_MINUTES));
            reservation.setSkateReservedAt(null);
            reservation.setSkateReservedBy(null);
            activated.add(reservationRepository.save(reservation));
        }
        return activated;
    }

    private List<Reservation> findLegacyReservationGroup(String code) {
        if (!code.regionMatches(true, 0, "RES-", 0, 4)) {
            return List.of();
        }
        try {
            Long reservationId = Long.valueOf(code.substring(4));
            return reservationRepository.findById(reservationId)
                    .map(List::of)
                    .orElseGet(List::of);
        } catch (NumberFormatException ex) {
            return List.of();
        }
    }

    private void ensureOwner(Reservation reservation, String username, boolean admin) {
        if (username != null
                && !admin
                && (reservation.getUser() == null
                || !username.equals(reservation.getUser().getUsername()))) {
            throw new AccessDeniedException("Bu bilet sizə aid deyil.");
        }
    }

    private void restoreSkate(Reservation reservation) {
        Skate skate = reservation.getSkate();
        if (skate == null) {
            return;
        }
        int total = skate.getTotalQuantity() == null ? Integer.MAX_VALUE : skate.getTotalQuantity();
        int available = skate.getAvailableQuantity() == null ? 0 : skate.getAvailableQuantity();
        skate.setAvailableQuantity(Math.min(total, available + 1));
        skateRepository.save(skate);
    }
}
