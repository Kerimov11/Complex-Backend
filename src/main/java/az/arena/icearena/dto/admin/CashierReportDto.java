package az.arena.icearena.dto.admin;

import az.arena.icearena.dto.PaymentDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierReportDto {
    private LocalDate date;
    private String cashierUsername;
    private BigDecimal cashTotal;
    private BigDecimal cardTotal;
    private BigDecimal totalAmount;
    private int transactionCount;
    private int ticketCount;
    private List<PaymentDto> sales;
}
