package az.arena.icearena.controller.coordinator;

import az.arena.icearena.dto.shift.ShiftSlotDto;
import az.arena.icearena.dto.shift.ShiftSlotRequest;
import az.arena.icearena.service.ShiftSlotService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coordinator/shift-slots")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class CoordinatorShiftSlotController {

    private final ShiftSlotService shiftSlotService;

    @GetMapping
    public ResponseEntity<List<ShiftSlotDto>> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(shiftSlotService.listAll(date));
    }

    @PostMapping
    public ResponseEntity<ShiftSlotDto> create(@Valid @RequestBody ShiftSlotRequest request) {
        return ResponseEntity.ok(shiftSlotService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftSlotDto> update(@PathVariable Long id,
                                               @Valid @RequestBody ShiftSlotRequest request) {
        return ResponseEntity.ok(shiftSlotService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shiftSlotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
