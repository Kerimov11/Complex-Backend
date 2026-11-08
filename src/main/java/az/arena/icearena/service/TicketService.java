// File: src/main/java/az/arena/icearena/service/TicketService.java
package az.arena.icearena.service;

import az.arena.icearena.dto.TicketDto;
import az.arena.icearena.dto.TicketPurchaseRequest;
import java.util.List;

public interface TicketService {
    List<TicketDto> purchaseTickets(TicketPurchaseRequest request, String username);

    List<TicketDto> getUserTickets(String username);

    TicketDto getMyTicketByCode(String username, String ticketCode);

    List<TicketDto> getTicketsBySession(Long sessionId);

    TicketDto getTicketByCode(String ticketCode);

    void validateTicket(String ticketCode);
}
