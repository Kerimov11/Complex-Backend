package az.arena.icearena.controller.volunteer;

import az.arena.icearena.dto.volunteer.VolunteerReservationDto;
import az.arena.icearena.service.VolunteerReservationService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteer/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
public class VolunteerReservationController {

    private final VolunteerReservationService volunteerReservationService;

    @GetMapping
    public ResponseEntity<List<VolunteerReservationDto>> list() {
        return ResponseEntity.ok(volunteerReservationService.listActiveReservations());
    }

    /** Canlı konki siqnalı: yalnız aktivləşdirilmiş və taymeri işləyən tələblər. */
    @GetMapping("/active")
    public ResponseEntity<List<VolunteerReservationDto>> active() {
        return ResponseEntity.ok(volunteerReservationService.listLiveReservations());
    }

    /** Könüllü konkini götürüb hazırladığını qeyd edir. */
    @PostMapping("/{id}/reserve")
    public ResponseEntity<VolunteerReservationDto> reserve(@PathVariable Long id,
                                                           Principal principal) {
        return ResponseEntity.ok(volunteerReservationService.reserve(id, principal.getName()));
    }

    @PostMapping("/{id}/fulfill")
    public ResponseEntity<VolunteerReservationDto> fulfill(@PathVariable Long id,
                                                           Principal principal) {
        return ResponseEntity.ok(volunteerReservationService.fulfill(id, principal.getName()));
    }
}
