package az.arena.icearena.controller.volunteer;

import az.arena.icearena.dto.volunteer.VolunteerInventoryDto;
import az.arena.icearena.service.VolunteerInventoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteer/skates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
public class VolunteerInventoryController {

    private final VolunteerInventoryService volunteerInventoryService;

    @GetMapping("/inventory")
    public ResponseEntity<List<VolunteerInventoryDto>> inventory() {
        return ResponseEntity.ok(volunteerInventoryService.listInventory());
    }
}
