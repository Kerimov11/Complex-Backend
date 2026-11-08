package az.arena.icearena.controller;

import az.arena.icearena.dto.SessionDto;
import az.arena.icearena.service.SessionService;
import java.time.LocalDate;
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
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SessionAdminController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionDto>> all() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<SessionDto>> byDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.getSessionsByDate(date));
    }

    @PostMapping
    public ResponseEntity<SessionDto> create(@RequestBody SessionDto dto) {
        return ResponseEntity.ok(sessionService.createSession(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionDto> update(@PathVariable Long id,
                                             @RequestBody SessionDto dto) {
        return ResponseEntity.ok(sessionService.updateSession(id, dto));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<SessionDto> block(@PathVariable Long id,
                                            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(sessionService.blockSession(id, reason));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<SessionDto> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.unblockSession(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
