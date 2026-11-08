// File: src/main/java/az/arena/icearena/dto/TicketPurchaseRequest.java
package az.arena.icearena.dto;

import az.arena.icearena.entity.TicketType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPurchaseRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Ticket type is required")
    private TicketType ticketType;

    @NotNull(message = "Number of tickets is required")
    private Integer quantity;
}
