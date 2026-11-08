package az.arena.icearena.controller.client;

import az.arena.icearena.dto.customer.CustomerRulesAcceptanceDto;
import az.arena.icearena.dto.customer.CustomerRulesAcceptanceRequest;
import az.arena.icearena.service.CustomerRulesAcceptanceService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Müştərinin qaydalar səhifəsindəki öhdəlik təsdiqlərini saxlayır.
 * Səhifə yeniləndikdə vəziyyət backend-dən bərpa olunur.
 */
@RestController
@RequestMapping("/api/customer/rules-acceptance")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerRulesAcceptanceController {

    private final CustomerRulesAcceptanceService acceptanceService;

    @GetMapping
    public ResponseEntity<CustomerRulesAcceptanceDto> get(Principal principal) {
        return ResponseEntity.ok(acceptanceService.getForUser(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<CustomerRulesAcceptanceDto> update(
            @Valid @RequestBody CustomerRulesAcceptanceRequest request,
            Principal principal) {
        return ResponseEntity.ok(acceptanceService.saveForUser(
                principal.getName(),
                request.getAcceptedObligationIds()));
    }
}
