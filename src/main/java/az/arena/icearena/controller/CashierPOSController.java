package az.arena.icearena.controller;

import az.arena.icearena.dto.PaymentDto;
import az.arena.icearena.dto.admin.CashierReportDto;
import az.arena.icearena.dto.admin.PosSaleRequest;
import az.arena.icearena.dto.admin.PosSaleResponse;
import az.arena.icearena.service.CashierPosService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/pos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
public class CashierPOSController {

    private final CashierPosService cashierPosService;

    @PostMapping("/sales")
    public ResponseEntity<PosSaleResponse> sell(@Valid @RequestBody PosSaleRequest request,
                                                Principal principal) {
        return ResponseEntity.ok(cashierPosService.sell(request, principal.getName()));
    }

    @GetMapping("/report")
    public ResponseEntity<CashierReportDto> report(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal,
            Authentication authentication) {
        return ResponseEntity.ok(cashierPosService.report(
                date, principal.getName(), isAdmin(authentication)));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<PaymentDto>> sales(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal,
            Authentication authentication) {
        return ResponseEntity.ok(cashierPosService.sales(
                date, principal.getName(), isAdmin(authentication)));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
