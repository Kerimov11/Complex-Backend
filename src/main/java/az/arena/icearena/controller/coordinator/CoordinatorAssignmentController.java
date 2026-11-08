package az.arena.icearena.controller.coordinator;

import az.arena.icearena.dto.shift.AssignStationRequest;
import az.arena.icearena.dto.shift.UpdateShiftRequest;
import az.arena.icearena.dto.shift.VolunteerShiftDto;
import az.arena.icearena.service.VolunteerShiftService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coordinator/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class CoordinatorAssignmentController {

    private final VolunteerShiftService volunteerShiftService;

    @PostMapping("/assign-station")
    public ResponseEntity<VolunteerShiftDto> assignStation(
            @Valid @RequestBody AssignStationRequest request,
            Principal principal) {
        return ResponseEntity.ok(volunteerShiftService.assignStation(
                request.getVolunteerShiftId(), request.getStation(), principal.getName()));
    }

    /** Könüllünü başqa smen vaxtına və/və ya stansiyaya keçirir. */
    @PutMapping("/{shiftId}")
    public ResponseEntity<VolunteerShiftDto> updateShift(
            @PathVariable Long shiftId,
            @RequestBody UpdateShiftRequest request,
            Principal principal) {
        return ResponseEntity.ok(volunteerShiftService.updateShift(
                shiftId, request.getShiftSlotId(), request.getStation(), principal.getName()));
    }

    /** Könüllünün seçdiyi smeni silir. */
    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long shiftId,
                                            Principal principal) {
        volunteerShiftService.deleteShift(shiftId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
