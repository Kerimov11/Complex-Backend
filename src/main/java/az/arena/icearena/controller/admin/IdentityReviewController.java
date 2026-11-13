package az.arena.icearena.controller.admin;

import az.arena.icearena.dto.admin.IdentityRejectRequest;
import az.arena.icearena.dto.admin.IdentityVerificationAdminDto;
import az.arena.icearena.service.IdentityReviewService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/identity-verifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR','CASHIER')")
public class IdentityReviewController {

    private final IdentityReviewService identityReviewService;

    @GetMapping
    public ResponseEntity<List<IdentityVerificationAdminDto>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(identityReviewService.list(status));
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<IdentityVerificationAdminDto> approve(
            @PathVariable Long userId,
            Principal principal) {
        return ResponseEntity.ok(identityReviewService.approve(userId, principal.getName()));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<IdentityVerificationAdminDto> reject(
            @PathVariable Long userId,
            @RequestBody(required = false) IdentityRejectRequest request,
            Principal principal) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(identityReviewService.reject(userId, reason, principal.getName()));
    }

    @PostMapping("/tickets/{ticketCode}/approve")
    public ResponseEntity<IdentityVerificationAdminDto> approveTicket(
            @PathVariable String ticketCode,
            Principal principal) {
        return ResponseEntity.ok(identityReviewService.approveTicket(ticketCode, principal.getName()));
    }

    @PostMapping("/tickets/{ticketCode}/reject")
    public ResponseEntity<IdentityVerificationAdminDto> rejectTicket(
            @PathVariable String ticketCode,
            @RequestBody(required = false) IdentityRejectRequest request,
            Principal principal) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(identityReviewService.rejectTicket(ticketCode, reason, principal.getName()));
    }
}
