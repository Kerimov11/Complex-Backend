package az.arena.icearena.controller.volunteer;

import az.arena.icearena.dto.shift.SelectShiftRequest;
import az.arena.icearena.dto.shift.ShiftSlotDto;
import az.arena.icearena.dto.shift.VolunteerShiftDto;
import az.arena.icearena.service.ShiftSlotService;
import az.arena.icearena.service.VolunteerShiftService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteer/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
public class VolunteerShiftController {

    private final ShiftSlotService shiftSlotService;
    private final VolunteerShiftService volunteerShiftService;

    @GetMapping("/available")
    public ResponseEntity<List<ShiftSlotDto>> available(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal) {
        return ResponseEntity.ok(shiftSlotService.listAvailable(date, principal.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<VolunteerShiftDto>> myShifts(Principal principal) {
        return ResponseEntity.ok(volunteerShiftService.listMine(principal.getName()));
    }

    @PostMapping("/select")
    public ResponseEntity<VolunteerShiftDto> select(
            @Valid @RequestBody SelectShiftRequest request,
            Principal principal) {
        return ResponseEntity.ok(volunteerShiftService.selectShift(
                principal.getName(), request.getShiftSlotId()));
    }
}
