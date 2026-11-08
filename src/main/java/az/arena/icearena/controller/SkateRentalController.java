// File: src/main/java/az/arena/icearena/controller/SkateRentalController.java
package az.arena.icearena.controller;

import az.arena.icearena.dto.SkateRentalDto;
import az.arena.icearena.dto.SkateRentalRequest;
import az.arena.icearena.service.SkateRentalService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skate-rentals")
@RequiredArgsConstructor
public class SkateRentalController {

    private final SkateRentalService skateRentalService;

    @PostMapping("/rent")
    public ResponseEntity<SkateRentalDto> rentSkates(@Valid @RequestBody SkateRentalRequest request,
                                                     Principal principal) {
        return ResponseEntity.ok(skateRentalService.rentSkates(request, principal.getName()));
    }

    @GetMapping("/my-rentals")
    public ResponseEntity<List<SkateRentalDto>> getMyRentals(Principal principal) {
        return ResponseEntity.ok(skateRentalService.getUserSkateRentals(principal.getName()));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SkateRentalDto>> getRentalsBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(skateRentalService.getSkateRentalsBySession(sessionId));
    }
}
