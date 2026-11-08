// File: src/main/java/az/arena/icearena/controller/TicketController.java
package az.arena.icearena.controller;

import az.arena.icearena.dto.TicketDto;
import az.arena.icearena.dto.TicketPurchaseRequest;
import az.arena.icearena.service.TicketService;
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
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    public ResponseEntity<List<TicketDto>> purchaseTickets(@Valid @RequestBody TicketPurchaseRequest request,
                                                            Principal principal) {
        return ResponseEntity.ok(ticketService.purchaseTickets(request, principal.getName()));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<TicketDto>> getMyTickets(Principal principal) {
        return ResponseEntity.ok(ticketService.getUserTickets(principal.getName()));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketDto>> getTicketsBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ticketService.getTicketsBySession(sessionId));
    }

    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketDto> getTicketByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.getTicketByCode(ticketCode));
    }

    @PostMapping("/validate/{ticketCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> validateTicket(@PathVariable String ticketCode) {
        ticketService.validateTicket(ticketCode);
        return ResponseEntity.ok("Ticket validated successfully");
    }
}
