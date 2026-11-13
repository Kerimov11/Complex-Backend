package az.arena.icearena.controller.client;

import az.arena.icearena.dto.customer.AgeVerificationRequest;
import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.dto.customer.IdentityVerificationRequest;
import az.arena.icearena.dto.customer.TicketPhotosRequest;
import az.arena.icearena.service.CustomerVerificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Müştərinin 18+ yaş və şəxsiyyət vəsiqəsi təsdiqi.
 */
@RestController
@RequestMapping("/api/customer/verification")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerVerificationController {

    private final CustomerVerificationService verificationService;

    @GetMapping
    public ResponseEntity<CustomerVerificationDto> get(Principal principal) {
        return ResponseEntity.ok(verificationService.getForUser(principal.getName()));
    }

    @PutMapping("/age")
    public ResponseEntity<CustomerVerificationDto> saveAge(
            @Valid @RequestBody AgeVerificationRequest request,
            Principal principal) {
        return ResponseEntity.ok(verificationService.saveAge(
                principal.getName(), request.getDateOfBirth()));
    }

    @PutMapping("/identity")
    public ResponseEntity<CustomerVerificationDto> saveIdentity(
            @Valid @RequestBody IdentityVerificationRequest request,
            Principal principal) {
        return ResponseEntity.ok(verificationService.saveIdentity(
                principal.getName(),
                request.getIdentityDocumentNumber(),
                request.getFinCode(),
                request.getIdentityPhotoData(),
                Boolean.TRUE.equals(request.getConfirmed())));
    }

    @PostMapping("/ticket-photos")
    public ResponseEntity<CustomerVerificationDto> saveTicketPhotos(
            @RequestBody TicketPhotosRequest request,
            Principal principal) {
        return ResponseEntity.ok(verificationService.saveTicketPhotos(
                principal.getName(), request));
    }

    @GetMapping("/push30-check")
    public ResponseEntity<java.util.Map<String, Boolean>> checkPush30Phone(
            @org.springframework.web.bind.annotation.RequestParam String phone) {
        return ResponseEntity.ok(java.util.Map.of(
                "used", verificationService.isPush30PhoneUsed(phone)));
    }
}
