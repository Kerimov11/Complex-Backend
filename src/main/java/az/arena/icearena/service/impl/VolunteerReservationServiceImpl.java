package az.arena.icearena.service.impl;

import az.arena.icearena.dto.volunteer.VolunteerReservationDto;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.service.ReservationActivationService;
import az.arena.icearena.service.VolunteerReservationService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VolunteerReservationServiceImpl implements VolunteerReservationService {

    private static final Set<Reservation.Status> VISIBLE_STATUSES = Set.of(
            Reservation.Status.PENDING,
            Reservation.Status.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final ReservationActivationService reservationActivationService;

    @Override
    @Transactional
    public List<VolunteerReservationDto> listActiveReservations() {
        reservationActivationService.expireStaleReservations();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return reservationRepository
                .findByStatusInAndSessionSessionDateGreaterThanEqualOrderBySessionSessionDateAscSessionStartTimeAscIdAsc(
                        VISIBLE_STATUSES, today)
                .stream()
                .filter(reservation -> isActiveOrUpcoming(reservation, today, now))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public List<VolunteerReservationDto> listLiveReservations() {
        reservationActivationService.expireStaleReservations();
        LocalDateTime now = LocalDateTime.now();
        return reservationRepository
                .findBySkateStockDeductedTrueAndSkateHandedOutAtIsNullAndTimerExpiresAtAfterOrderByTimerExpiresAtAsc(
                        now)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public VolunteerReservationDto reserve(Long reservationId, String username) {
        reservationActivationService.expireStaleReservations();
        Reservation reservation = find(reservationId);
        ensureLive(reservation);

        if (reservation.getSkateReservedAt() == null) {
            reservation.setSkateReservedAt(LocalDateTime.now());
            reservation.setSkateReservedBy(username);
            reservationRepository.save(reservation);
        }
        return toDto(reservation);
    }

    @Override
    @Transactional
    public VolunteerReservationDto fulfill(Long reservationId, String username) {
        reservationActivationService.expireStaleReservations();
        Reservation reservation = find(reservationId);

        // Təkrar klik idempotentdir: artıq təhvil verilmiş konki yenidən
        // stokdan çıxılmır və taymer dəyişdirilmir.
        if (reservation.getSkateHandedOutAt() != null) {
            return toDto(reservation);
        }

        ensureLive(reservation);

        if (reservation.getSkateReservedAt() == null) {
            throw new BadRequestException(
                    "Əvvəlcə \"Konki hazırdır\" düyməsi ilə konkini ayırın.");
        }

        LocalDateTime now = LocalDateTime.now();
        reservation.setSkateHandedOutAt(now);
        reservation.setSkateHandedOutBy(username);
        // Təhvil verildikdən sonra taymer dayanır, stok istifadədə qalır.
        reservation.setTimerExpiresAt(null);
        reservationRepository.save(reservation);
        return toDto(reservation);
    }

    private Reservation find(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rezervasiya tapılmadı: " + reservationId));
    }

    private void ensureLive(Reservation reservation) {
        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new BadRequestException(
                    "Yalnız təsdiqlənmiş rezervasiya üçün konki hazırlana bilər.");
        }
        if (reservation.getSkateHandedOutAt() != null) {
            throw new BadRequestException("Konki artıq təhvil verilib.");
        }
        if (!Boolean.TRUE.equals(reservation.getSkateStockDeducted())
                || reservation.getTimerExpiresAt() == null
                || !reservation.getTimerExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Konki rezervasiyası aktiv deyil. Müştəri taymeri aktivləşdirməlidir.");
        }
    }

    private boolean isActiveOrUpcoming(Reservation reservation, LocalDate today, LocalTime now) {
        Session session = reservation.getSession();
        if (session == null || session.getSessionDate() == null) {
            return false;
        }
        if (session.getSessionDate().isAfter(today)) {
            return true;
        }
        return session.getSessionDate().equals(today)
                && session.getEndTime() != null
                && session.getEndTime().isAfter(now);
    }

    private VolunteerReservationDto toDto(Reservation reservation) {
        User customer = reservation.getUser();
        Session session = reservation.getSession();
        Skate skate = reservation.getSkate();
        LocalDateTime now = LocalDateTime.now();
        boolean active = Boolean.TRUE.equals(reservation.getSkateStockDeducted())
                && reservation.getSkateHandedOutAt() == null
                && reservation.getTimerExpiresAt() != null
                && reservation.getTimerExpiresAt().isAfter(now);
        int activationCount = reservation.getReservationExtensionCount() == null
                ? 0
                : reservation.getReservationExtensionCount();
        long remainingSeconds = active
                ? Math.max(0, Duration.between(now, reservation.getTimerExpiresAt()).getSeconds())
                : 0;
        return VolunteerReservationDto.builder()
                .id(reservation.getId())
                .customerId(customer == null ? null : customer.getId())
                .customerName(customerName(customer))
                .customerUsername(customer == null ? null : customer.getUsername())
                .sessionId(session == null ? null : session.getId())
                .sessionDate(session == null ? null : session.getSessionDate())
                .sessionStartTime(session == null ? null : session.getStartTime())
                .sessionEndTime(session == null ? null : session.getEndTime())
                .entryTime(reservation.getEntryTime())
                .durationMinutes(reservation.getDurationMinutes())
                .skateSize(skate == null ? null : skate.getSize())
                .status(reservation.getStatus())
                .statusLabel(statusLabel(reservation, active))
                .skateHandedOut(reservation.getSkateHandedOutAt() != null)
                .skateHandedOutAt(reservation.getSkateHandedOutAt())
                .skateHandedOutBy(reservation.getSkateHandedOutBy())
                .voucherCode(reservation.getVoucherCode())
                .activationCount(activationCount)
                .remainingActivations(Math.max(0,
                        ReservationActivationServiceImpl.MAX_ACTIVATION_ATTEMPTS - activationCount))
                .timerExpiresAt(reservation.getTimerExpiresAt())
                .timerRemainingSeconds(remainingSeconds)
                .reservationActive(active)
                .skateStockDeducted(Boolean.TRUE.equals(reservation.getSkateStockDeducted()))
                .skateReserved(reservation.getSkateReservedAt() != null)
                .skateReservedAt(reservation.getSkateReservedAt())
                .skateReservedBy(reservation.getSkateReservedBy())
                .build();
    }

    private String statusLabel(Reservation reservation, boolean active) {
        if (reservation.getSkateHandedOutAt() != null) {
            return "Təhvil verilib";
        }
        if (active) {
            return reservation.getSkateReservedAt() == null
                    ? "Yeni konki tələbi"
                    : "Konki hazırlanır";
        }
        return statusLabel(reservation.getStatus());
    }

    private String customerName(User customer) {
        if (customer == null) {
            return "Naməlum müştəri";
        }
        String fullName = ((customer.getFirstName() == null ? "" : customer.getFirstName())
                + " "
                + (customer.getLastName() == null ? "" : customer.getLastName())).trim();
        return fullName.isEmpty() ? customer.getUsername() : fullName;
    }

    private String statusLabel(Reservation.Status status) {
        if (status == null) {
            return "Naməlum";
        }
        return switch (status) {
            case PENDING -> "Ödəniş gözləyir";
            case CONFIRMED -> "Təsdiqlənib";
            case CANCELLED -> "Ləğv edilib";
            case EXPIRED -> "Vaxtı bitib";
        };
    }
}
