package az.arena.icearena.controller;

import az.arena.icearena.dto.SkateDto;
import az.arena.icearena.dto.admin.SkateStockRequest;
import az.arena.icearena.service.SkateAdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/skates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SkateAdminController {

    private final SkateAdminService skateAdminService;

    @GetMapping
    public ResponseEntity<List<SkateDto>> list() {
        return ResponseEntity.ok(skateAdminService.list());
    }

    @PostMapping
    public ResponseEntity<SkateDto> create(@Valid @RequestBody SkateStockRequest request) {
        return ResponseEntity.ok(skateAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkateDto> updateStock(@PathVariable Long id,
                                                @Valid @RequestBody SkateStockRequest request) {
        return ResponseEntity.ok(skateAdminService.updateStock(id, request));
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<SkateDto> restock(@PathVariable Long id,
                                            @RequestParam int quantity) {
        return ResponseEntity.ok(skateAdminService.restock(id, quantity));
    }

    @PostMapping("/{id}/write-off")
    public ResponseEntity<SkateDto> writeOff(@PathVariable Long id,
                                             @RequestParam int quantity) {
        return ResponseEntity.ok(skateAdminService.writeOff(id, quantity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        skateAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
