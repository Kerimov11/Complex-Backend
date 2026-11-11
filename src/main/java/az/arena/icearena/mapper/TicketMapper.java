// File: src/main/java/az/arena/icearena/mapper/TicketMapper.java
package az.arena.icearena.mapper;

import az.arena.icearena.dto.TicketDto;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public TicketDto toDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        Session session = ticket.getSession();
        boolean used = Boolean.TRUE.equals(ticket.getIsUsed());
        boolean past = session != null
                && session.getSessionDate() != null
                && session.getEndTime() != null
                && LocalDateTime.of(session.getSessionDate(), session.getEndTime())
                .isBefore(LocalDateTime.now());
        // Bilet istifadə edilməyənədək (QR skan edilənədək) qeyri-məhdud müddətə aktivdir
        String status = used ? "USED" : "ACTIVE";
        return TicketDto.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .qrCode(ticket.getQrCode())
                .ticketType(ticket.getTicketType())
                .price(ticket.getPrice())
                .purchaseDate(ticket.getPurchaseDate())
                .isUsed(ticket.getIsUsed())
                .sessionId(session != null ? session.getId() : null)
                .username(ticket.getUser() != null ? ticket.getUser().getUsername() : null)
                .sessionDate(session != null ? session.getSessionDate() : null)
                .sessionStartTime(session != null ? session.getStartTime() : null)
                .sessionEndTime(session != null ? session.getEndTime() : null)
                .status(status)
                .statusLabel(statusLabel(status))
                .source("TICKET")
                .participantCount(1)
                .activationCount(0)
                .maxActivations(0)
                .remainingActivations(0)
                .canActivate(false)
                .reservationActive(false)
                .skateStockDeducted(false)
                .skateHandedOut(false)
                .reservationIds(List.of())
                .skateSizes(List.of())
                .build();
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "USED" -> "İstifadə edilib";
            case "EXPIRED" -> "Vaxtı bitib";
            default -> "Aktiv";
        };
    }
}
