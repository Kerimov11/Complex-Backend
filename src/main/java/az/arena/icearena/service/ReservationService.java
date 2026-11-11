package az.arena.icearena.service;

import az.arena.icearena.dto.SessionAvailabilityDto;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SkateRepository skateRepository;
    private final SessionTimeService sessionTimeService;
    private final ReservationActivationService reservationActivationService;
    private final CustomerVerificationService customerVerificationService;

    /** Köhnə çağırışlar üçün uyğunluq. */
    @Transactional
    public Reservation createReservation(Long userId, Long sessionId, Long skateId) {
        return createReservation(userId, sessionId, skateId, null, 60, null, false);
    }

    @Transactional
    public Reservation createReservation(Long userId,
                                         Long sessionId,
                                         Long skateId,
                                         LocalTime entryTime,
                                         Integer durationMinutes,
                                         String username,
                                         boolean admin) {
        return createReservation(userId, sessionId, skateId, entryTime,
                durationMinutes, username, admin, null);
    }

    @Transactional
    public Reservation createReservation(Long userId,
                                         Long sessionId,
                                         Long skateId,
                                         LocalTime entryTime,
                                         Integer durationMinutes,
                                         String username,
                                         boolean admin,
                                         String voucherCode) {
        return createReservation(userId, sessionId, skateId, entryTime,
                durationMinutes, username, admin, voucherCode, null, null);
    }

    @Transactional
    public Reservation createReservation(Long userId,
                                         Long sessionId,
                                         Long skateId,
                                         LocalTime entryTime,
                                         Integer durationMinutes,
                                         String username,
                                         boolean admin,
                                         String voucherCode,
                                         String participantName,
                                         Integer participantAge) {
        User user = resolveUser(userId, username, admin);
        if (username != null && !admin) {
            customerVerificationService.requireTicketPurchaseEligibility(user);
        }
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Seans tapılmadı: " + sessionId));
        Skate skate = skateRepository.findById(skateId)
                .orElseThrow(() -> new ResourceNotFoundException("Konki tapılmadı: " + skateId));

        LocalDateTime now = LocalDateTime.now();
        LocalTime effectiveEntry = resolveEntryTime(session, entryTime, now);
        int duration = normalizeDuration(durationMinutes);

        // Onlayn biletlər sərbəst vaxtlı və limitsiz etibarlıdır; cari sessiyanın bağlanması və ya qalıq vaxt
        // onlayn bilet alışını bloklamır (müştəri QR skan edildikdən sonra istədiyi vaxt buzda sürüşə bilər).
        if (session.getAvailableTickets() != null && session.getAvailableTickets() > 0) {
            session.setAvailableTickets(session.getAvailableTickets() - 1);
            sessionRepository.save(session);
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .session(session)
                .skate(skate)
                .status(Reservation.Status.PENDING)
                .createdAt(now)
                .entryTime(effectiveEntry)
                .durationMinutes(duration)
                .reservationExtensionCount(0)
                .skateStockDeducted(false)
                .voucherCode(normalizeVoucherCode(voucherCode))
                .participantName(normalizeParticipantName(participantName))
                .participantAge(participantAge)
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation extendReservation(Long reservationId) {
        return extendReservation(reservationId, null, true);
    }

    /**
     * Köhnə `/extend` endpointi artıq konki rezervasiyasını əl ilə
     * aktivləşdirir. Stok yalnız bu çağırışda çıxılır.
     */
    @Transactional
    public Reservation extendReservation(Long reservationId, String username, boolean admin) {
        List<Reservation> activated = reservationActivationService
                .activateReservation(reservationId, username, admin);
        return activated.isEmpty()
                ? getOwnedReservation(reservationId, username, admin)
                : activated.get(0);
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasiya tapılmadı: " + reservationId));
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(Long reservationId, String username, boolean admin) {
        return getOwnedReservation(reservationId, username, admin);
    }

    /** Frontend çoxnəfərli sifarişin bir hissəsi uğursuz olduqda çağırır. */
    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        return cancelReservationInternal(reservationId);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, String username, boolean admin) {
        Reservation reservation = getOwnedReservation(reservationId, username, admin);
        return cancelReservationInternal(reservation);
    }

    /**
     * Ödəniş uğursuz olduqda bütün rezervasiyaları ləğv edir və stoku qaytarır.
     * Artıq ləğv edilmiş rezervasiya təkrar stok artırmır (idempotent).
     */
    @Transactional
    public void cancelReservations(Collection<Long> reservationIds) {
        if (reservationIds == null) {
            return;
        }
        for (Long id : reservationIds) {
            if (id == null) {
                continue;
            }
            reservationRepository.findById(id).ifPresent(this::cancelReservationInternal);
        }
    }

    /** Ödəniş uğurlu olduqda rezervasiyaları təsdiqlənmiş statusuna keçirir. */
    @Transactional
    public void confirmReservations(Collection<Long> reservationIds) {
        if (reservationIds == null) {
            return;
        }
        for (Long id : reservationIds) {
            if (id == null) {
                continue;
            }
            reservationRepository.findById(id).ifPresent(reservation -> {
                if (reservation.getStatus() == Reservation.Status.PENDING) {
                    reservation.setStatus(Reservation.Status.CONFIRMED);
                    reservationRepository.save(reservation);
                }
            });
        }
    }

    private Reservation cancelReservationInternal(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasiya tapılmadı: " + reservationId));
        return cancelReservationInternal(reservation);
    }

    private Reservation cancelReservationInternal(Reservation reservation) {
        if (reservation.getStatus() == Reservation.Status.PENDING
                || reservation.getStatus() == Reservation.Status.CONFIRMED) {
            if (Boolean.TRUE.equals(reservation.getSkateStockDeducted())) {
                Skate skate = reservation.getSkate();
                if (skate != null) {
                    int total = skate.getTotalQuantity() == null
                            ? Integer.MAX_VALUE
                            : skate.getTotalQuantity();
                    int available = skate.getAvailableQuantity() == null
                            ? 0
                            : skate.getAvailableQuantity();
                    skate.setAvailableQuantity(Math.min(total, available + 1));
                    skateRepository.save(skate);
                }
            }

            Session session = reservation.getSession();
            if (session != null) {
                int max = session.getMaxTicketLimit() == null
                        ? Integer.MAX_VALUE
                        : session.getMaxTicketLimit();
                int available = session.getAvailableTickets() == null
                        ? 0
                        : session.getAvailableTickets();
                session.setAvailableTickets(Math.min(max, available + 1));
                sessionRepository.save(session);
            }
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservation.setTimerExpiresAt(null);
        reservation.setSkateStockDeducted(false);
        reservation.setSkateReservedAt(null);
        reservation.setSkateReservedBy(null);
        return reservationRepository.save(reservation);
    }

    private Reservation getOwnedReservation(Long reservationId, String username, boolean admin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasiya tapılmadı: " + reservationId));
        if (username != null
                && !admin
                && (reservation.getUser() == null
                || !username.equals(reservation.getUser().getUsername()))) {
            throw new AccessDeniedException("Bu rezervasiya sizə aid deyil.");
        }
        return reservation;
    }

    private User resolveUser(Long userId, String username, boolean admin) {
        if (username == null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + userId));
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + username));
        if (userId != null && !user.getId().equals(userId) && !admin) {
            throw new AccessDeniedException("Başqa istifadəçi adına rezervasiya yaratmaq olmaz.");
        }
        return user;
    }

    private LocalTime resolveEntryTime(Session session, LocalTime entryTime, LocalDateTime now) {
        if (entryTime != null) {
            return entryTime;
        }
        LocalDate today = now.toLocalDate();
        if (session.getSessionDate().equals(today)) {
            if (now.toLocalTime().isBefore(session.getStartTime())) {
                return session.getStartTime();
            }
            if (now.toLocalTime().isAfter(session.getEndTime())) {
                return session.getEndTime();
            }
            return now.toLocalTime();
        }
        return session.getStartTime();
    }

    private int normalizeDuration(Integer duration) {
        if (duration == null) {
            return 60;
        }
        if (duration != 30 && duration != 60) {
            throw new BadRequestException("Bilet müddəti yalnız 30 və ya 60 dəqiqə ola bilər.");
        }
        return duration;
    }

    private String normalizeVoucherCode(String voucherCode) {
        if (voucherCode == null) {
            return null;
        }
        String trimmed = voucherCode.trim();
        return trimmed.isEmpty() ? null : trimmed.substring(0, Math.min(100, trimmed.length()));
    }

    private String normalizeParticipantName(String participantName) {
        if (participantName == null) {
            return null;
        }
        String trimmed = participantName.trim();
        return trimmed.isEmpty() ? null : trimmed.substring(0, Math.min(100, trimmed.length()));
    }
}
