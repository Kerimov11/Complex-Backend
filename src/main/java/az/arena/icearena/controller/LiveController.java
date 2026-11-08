package az.arena.icearena.controller;

import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ana səhifədəki "canlı" göstərici üçün public endpoint.
 * Hazırda buzdakı müştəri sayı aktiv seansdaki PENDING/CONFIRMED rezervasiyalardan hesablanır.
 */
@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveController {

    private final SessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLiveStats() {
        LocalDateTime now = LocalDateTime.now();
        List<Session> currentSessions = sessionRepository.findBySessionDate(now.toLocalDate()).stream()
                .filter(s -> !now.toLocalTime().isBefore(s.getStartTime())
                        && now.toLocalTime().isBefore(s.getEndTime()))
                .toList();

        long onIce = currentSessions.stream()
                .mapToLong(s -> reservationRepository.countBySessionIdAndStatusIn(
                        s.getId(),
                        List.of(Reservation.Status.PENDING, Reservation.Status.CONFIRMED)))
                .sum();

        return ResponseEntity.ok(Map.of(
                "iceActive", !currentSessions.isEmpty(),
                "onIce", onIce,
                "updatedAt", now.toString(),
                "source", "reservations"
        ));
    }
}
