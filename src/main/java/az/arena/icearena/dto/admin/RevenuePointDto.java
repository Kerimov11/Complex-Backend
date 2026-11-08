package az.arena.icearena.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenuePointDto {
    private LocalDate date;
    private BigDecimal amount;
    private long transactions;
}
