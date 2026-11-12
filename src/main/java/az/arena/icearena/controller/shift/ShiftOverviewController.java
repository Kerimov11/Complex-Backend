package az.arena.icearena.controller.shift;

import az.arena.icearena.dto.shift.ShiftOverviewDto;
import az.arena.icearena.service.ShiftOverviewService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class ShiftOverviewController {

    private final ShiftOverviewService shiftOverviewService;

    @GetMapping("/overview")
    public ResponseEntity<ShiftOverviewDto> overview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(shiftOverviewService.overview(date));
    }
}
