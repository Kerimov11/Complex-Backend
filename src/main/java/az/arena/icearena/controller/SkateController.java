package az.arena.icearena.controller;

import az.arena.icearena.dto.SkateDto;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.service.ReservationActivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Konki ölçüləri və mövcudluğu — frontend-in ölçüyə uyğun skateId seçməsi üçün.
 * Public endpoint (auth tələb etmir).
 */
@RestController
@RequestMapping("/api/skates")
@RequiredArgsConstructor
@Tag(name = "Skates", description = "Konki ölçüləri, ümumi və sessiya üzrə stok")
public class SkateController {

    private final SkateRepository skateRepository;
    private final SessionRepository sessionRepository;
    private final ReservationActivationService reservationActivationService;

    @GetMapping
    @Operation(summary = "Bütün konki ölçüləri və ümumi mövcud stok")
    public ResponseEntity<List<SkateDto>> getAllSkates() {
        reservationActivationService.expireStaleReservations();
        List<SkateDto> skates = skateRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(skates);
    }

    /**
     * Konkret sessiya üçün stok. Onlayn bilet alınanda konki dərhal stokdan çıxılmır —
     * yalnız müştəri konki rezervasiyasını aktivləşdirdikdə və ya könüllü/əməkdaş
     * "Konki təhvil verildi" düyməsini basdıqda stokdan azalır.
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Konkret sessiya üçün qalan konki stoku")
    public ResponseEntity<List<SkateDto>> getSkatesBySession(@PathVariable Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found with id: " + sessionId);
        }
        reservationActivationService.expireStaleReservations();

        List<SkateDto> skates = skateRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(skates);
    }

    private SkateDto toDto(Skate skate) {
        return SkateDto.builder()
                .id(skate.getId())
                .size(skate.getSize())
                .totalQuantity(skate.getTotalQuantity())
                .availableQuantity(skate.getAvailableQuantity())
                .damagedQuantity(skate.getDamagedQuantity())
                .build();
    }
}
