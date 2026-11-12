package az.arena.icearena.controller.admin;

import az.arena.icearena.dto.admin.CreateStaffRequest;
import az.arena.icearena.dto.admin.StaffRoleUpdateRequest;
import az.arena.icearena.dto.admin.UpdateStaffRequest;
import az.arena.icearena.dto.admin.UserAdminDto;
import az.arena.icearena.service.UserAdminService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping("/staff")
    public ResponseEntity<List<UserAdminDto>> listStaff(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(userAdminService.listStaff(q));
    }

    @PostMapping("/staff/create")
    public ResponseEntity<UserAdminDto> createStaffMember(
            @Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(userAdminService.createStaffMember(request));
    }

    @PutMapping("/staff/{id}/role")
    public ResponseEntity<UserAdminDto> updateStaffRole(
            @PathVariable Long id,
            @Valid @RequestBody StaffRoleUpdateRequest request) {
        return ResponseEntity.ok(userAdminService.updateStaffRole(id, request.getRole()));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<UserAdminDto> updateStaffMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(userAdminService.updateStaffMember(id, request));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaffMember(
            @PathVariable Long id,
            Principal principal) {
        String actorUsername = principal == null ? null : principal.getName();
        userAdminService.deleteStaffMember(id, actorUsername);
        return ResponseEntity.noContent().build();
    }
}
