package az.arena.icearena.controller.admin;

import az.arena.icearena.dto.admin.CreateStaffRequest;
import az.arena.icearena.dto.admin.StaffRoleUpdateRequest;
import az.arena.icearena.dto.admin.UserAdminDto;
import az.arena.icearena.dto.admin.UserRoleUpdateRequest;
import az.arena.icearena.entity.RoleName;
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

    @GetMapping("/users")
    public ResponseEntity<List<UserAdminDto>> list(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(userAdminService.listUsers(q));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getUser(id));
    }

    @PostMapping("/users")
    public ResponseEntity<UserAdminDto> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(userAdminService.createStaff(request));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserAdminDto> updateRoles(@PathVariable Long id,
                                                    @Valid @RequestBody UserRoleUpdateRequest request,
                                                    Principal principal) {
        return ResponseEntity.ok(userAdminService.updateRoles(id, request, principal.getName()));
    }

    @PostMapping("/users/{id}/roles/{role}")
    public ResponseEntity<UserAdminDto> addRole(@PathVariable Long id,
                                                @PathVariable RoleName role) {
        return ResponseEntity.ok(userAdminService.addRole(id, role));
    }

    @DeleteMapping("/users/{id}/roles/{role}")
    public ResponseEntity<UserAdminDto> removeRole(@PathVariable Long id,
                                                   @PathVariable RoleName role,
                                                   Principal principal) {
        return ResponseEntity.ok(userAdminService.removeRole(id, role, principal.getName()));
    }

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
}
