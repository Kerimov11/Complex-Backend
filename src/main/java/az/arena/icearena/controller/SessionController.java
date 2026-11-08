package az.arena.icearena.controller;

import az.arena.icearena.dto.SessionAvailabilityDto;
import az.arena.icearena.dto.SessionDto;
import az.arena.icearena.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Sessiyalar, iş saatları və dinamik bilet müddəti")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/{sessionId}")
    @Operation(summary = "Sessiya məlumatı")
    public ResponseEntity<SessionDto> getSessionById(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSessionById(sessionId));
    }

    @GetMapping
    @Operation(summary = "Bütün sessiyalar")
    public ResponseEntity<List<SessionDto>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    /**
     * Frontend-in istifadə etdiyi əsas endpoint. Cavabda hər sessiya üçün
     * remainingMinutes, salesOpen və availableDurations da qaytarılır.
     */
    @GetMapping("/public/date")
    @Operation(summary = "Tarix üzrə sessiyalar + dinamik qalıq vaxt")
    public ResponseEntity<List<SessionDto>> getSessionsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate) {
        return ResponseEntity.ok(sessionService.getSessionsByDate(sessionDate));
    }

    @GetMapping("/public/range")
    @Operation(summary = "Tarix aralığı üzrə sessiyalar")
    public ResponseEntity<List<SessionDto>> getSessionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(sessionService.getSessionsByDateRange(startDate, endDate));
    }

    /**
     * 13:15 üçün 13:45-də bağlanan sessiya: remainingMinutes=30,
     * availableDurations=[30] qaytarır. 13:20 üçün salesOpen=false olur.
     */
    @GetMapping("/public/{sessionId}/availability")
    @Operation(summary = "Sessiya üçün dinamik bilet müddəti yoxlaması")
    public ResponseEntity<SessionAvailabilityDto> getSessionAvailability(
            @PathVariable Long sessionId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) Integer durationMinutes) {
        return ResponseEntity.ok(sessionService.getSessionAvailability(sessionId, startTime, durationMinutes));
    }
}
