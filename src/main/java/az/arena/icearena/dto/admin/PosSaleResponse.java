package az.arena.icearena.dto.admin;

import az.arena.icearena.entity.PaymentMethod;
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
public class PosSaleResponse {
    private String saleReference;
    private BigDecimal totalAmount;
    private BigDecimal unitPrice;
    private PaymentMethod paymentMethod;
    private String cashierUsername;
    private String customerName;
    private Long sessionId;
    private LocalDateTime soldAt;
    private int quantity;
    private List<PosTicketDto> tickets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PosTicketDto {
        private Long id;
        private String ticketCode;
        private String qrCode;
        private BigDecimal price;
    }
}
