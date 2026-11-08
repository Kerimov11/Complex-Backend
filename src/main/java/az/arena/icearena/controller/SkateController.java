package az.arena.icearena.controller;

import az.arena.icearena.dto.SkateDto;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
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

    private static final Set<Reservation.Status> ACTIVE_STATUSES =
            Set.of(Reservation.Status.PENDING, Reservation.Status.CONFIRMED);

    private final SkateRepository skateRepository;
    private final SessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping
    @Operation(summary = "Bütün konki ölçüləri və ümumi mövcud stok")
    public ResponseEntity<List<SkateDto>> getAllSkates() {
        List<SkateDto> skates = skateRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(skates);
    }

    /**
     * Konkret sessiya üçün stok. Bir ölçüdə artıq rezerv olunmuş cütlər
     * çıxılır; sessiyalar üst-üstə düşmədiyi üçün digər sessiyaların
     * rezervləri bu sessiyanın stokunu azaltmır.
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Konkret sessiya üçün qalan konki stoku")
    public ResponseEntity<List<SkateDto>> getSkatesBySession(@PathVariable Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        List<SkateDto> skates = skateRepository.findAll().stream()
                .map(skate -> {
                    long reserved = reservationRepository
                            .countBySessionIdAndSkateIdAndStatusIn(
                                    session.getId(), skate.getId(), ACTIVE_STATUSES);
                    int total = skate.getTotalQuantity() == null ? 0 : skate.getTotalQuantity();
                    int available = Math.max(0, total - (int) reserved);
                    return SkateDto.builder()
                            .id(skate.getId())
                            .size(skate.getSize())
                            .totalQuantity(total)
                            .availableQuantity(available)
                            .damagedQuantity(skate.getDamagedQuantity())
                            .build();
                })
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
