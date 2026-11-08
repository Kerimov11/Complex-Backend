package az.arena.icearena.controller;

import az.arena.icearena.dto.admin.VerificationRequest;
import az.arena.icearena.dto.admin.VerificationResultDto;
import az.arena.icearena.service.VerificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR','CASHIER','VOLUNTEER')")
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/scan")
    public ResponseEntity<VerificationResultDto> scan(@Valid @RequestBody VerificationRequest request,
                                                      Principal principal) {
        return ResponseEntity.ok(verificationService.verify(request.getCode(), principal.getName()));
    }

    @GetMapping("/lookup")
    public ResponseEntity<VerificationResultDto> lookup(@RequestParam String code) {
        return ResponseEntity.ok(verificationService.lookup(code));
    }
}
