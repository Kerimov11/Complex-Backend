package az.arena.icearena.dto;

import az.arena.icearena.entity.PaymentChannel;
import az.arena.icearena.entity.PaymentMethod;
import az.arena.icearena.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private Long id;
    private String transactionId;
    private String reference;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
    private LocalDateTime completionDate;
    private Long userId;
    private String username;
    private Long ticketId;
    private Long skateRentalId;
    private List<Long> reservationIds;
    private PaymentChannel channel;
    private PaymentMethod paymentMethod;
    private String cashierUsername;
    private String customerName;
    private Integer quantity;
}
