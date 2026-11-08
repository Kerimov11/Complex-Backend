package az.arena.icearena.controller.client;

import az.arena.icearena.dto.TicketDto;
import az.arena.icearena.service.ReservationActivationService;
import az.arena.icearena.service.TicketService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Müştəri tərəfində konki rezervasiyasının əl ilə aktivləşdirilməsi.
 * İlk aktivləşdirmə də daxil olmaqla maksimum 2 cəhd mümkündür.
 */
@RestController
@RequestMapping("/api/customer/reservations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerReservationController {

    private final ReservationActivationService reservationActivationService;
    private final TicketService ticketService;

    @PostMapping("/{ticketCode}/activate")
    public ResponseEntity<TicketDto> activate(@PathVariable String ticketCode,
                                              Principal principal) {
        reservationActivationService.activateByCode(ticketCode, principal.getName(), false);
        return ResponseEntity.ok(ticketService.getMyTicketByCode(principal.getName(), ticketCode));
    }
}
