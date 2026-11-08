package az.arena.icearena.controller;

import az.arena.icearena.dto.DiscountBreakdownDto;
import az.arena.icearena.dto.DiscountRequest;
import az.arena.icearena.dto.DiscountRuleDto;
import az.arena.icearena.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts", description = "Avtomatik endirim mühərriki və admin qaydaları")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping("/active")
    @Operation(summary = "Aktiv endirim qaydaları (public)")
    public ResponseEntity<List<DiscountRuleDto>> getActiveRules() {
        return ResponseEntity.ok(discountService.getActiveRules());
    }

    @PostMapping("/apply")
    @Operation(summary = "Səbət üçün avtomatik endirim hesablaması (public)")
    public ResponseEntity<DiscountBreakdownDto> applyDiscount(
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(discountService.calculate(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bütün endirim qaydaları (admin)")
    public ResponseEntity<List<DiscountRuleDto>> getAllRules() {
        return ResponseEntity.ok(discountService.getAllRules());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Yeni endirim qaydası yarat (admin)")
    public ResponseEntity<DiscountRuleDto> createRule(@RequestBody DiscountRuleDto dto) {
        return ResponseEntity.ok(discountService.createRule(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Endirim qaydasını yenilə (admin)")
    public ResponseEntity<DiscountRuleDto> updateRule(@PathVariable Long id,
                                                      @RequestBody DiscountRuleDto dto) {
        return ResponseEntity.ok(discountService.updateRule(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Endirim qaydasını sil (admin)")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        discountService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
