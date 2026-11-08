package az.arena.icearena.controller;

import az.arena.icearena.entity.Reservation;
import az.arena.icearena.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Konki rezervasiyası, taymer və ləğv")
public class ReservationController {

    private final ReservationService reservationService;

    /** Köhnə brauzer testləri üçün saxlanılır. */
    @GetMapping("/create")
    @Operation(summary = "Rezervasiya yarat (köhnə GET variantı)")
    public ResponseEntity<Reservation> createReservation(
            @RequestParam(required = false) Long userId,
            @RequestParam Long sessionId,
            @RequestParam Long skateId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) String voucherCode,
            Principal principal,
            Authentication authentication) {
        return ResponseEntity.ok(create(userId, sessionId, skateId, startTime,
                durationMinutes, voucherCode, principal, authentication));
    }

    /** Frontend üçün əsas yaratma endpoint-i — state dəyişdiyi üçün POST. */
    @PostMapping("/create")
    @Operation(summary = "Rezervasiya yarat + dinamik bilet müddəti yoxlaması")
    public ResponseEntity<Reservation> createReservationPost(
            @RequestParam(required = false) Long userId,
            @RequestParam Long sessionId,
            @RequestParam Long skateId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) String voucherCode,
            Principal principal,
            Authentication authentication) {
        return ResponseEntity.ok(create(userId, sessionId, skateId, startTime,
                durationMinutes, voucherCode, principal, authentication));
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Rezervasiya məlumatı")
    public ResponseEntity<Reservation> getReservation(@PathVariable Long reservationId,
                                                      Principal principal,
                                                      Authentication authentication) {
        return ResponseEntity.ok(reservationService.getReservation(
                reservationId, principal.getName(), isAdmin(authentication)));
    }

    /** Taymeri 10 dəqiqə uzadır — maksimum 2 dəfə. */
    @PostMapping("/{reservationId}/extend")
    @Operation(summary = "Rezervasiya taymerini 10 dəqiqə uzat")
    public ResponseEntity<Reservation> extendReservation(@PathVariable Long reservationId,
                                                         Principal principal,
                                                         Authentication authentication) {
        return ResponseEntity.ok(reservationService.extendReservation(
                reservationId, principal.getName(), isAdmin(authentication)));
    }

    /** Rezervasiyanı ləğv edir və konki stokunu geri qaytarır. */
    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "Rezervasiyanı ləğv et və stoku qaytar")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long reservationId,
                                                         Principal principal,
                                                         Authentication authentication) {
        return ResponseEntity.ok(reservationService.cancelReservation(
                reservationId, principal.getName(), isAdmin(authentication)));
    }

    private Reservation create(Long userId,
                               Long sessionId,
                               Long skateId,
                               LocalTime startTime,
                               Integer durationMinutes,
                               String voucherCode,
                               Principal principal,
                               Authentication authentication) {
        return reservationService.createReservation(
                userId, sessionId, skateId, startTime, durationMinutes,
                principal.getName(), isAdmin(authentication), voucherCode);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
