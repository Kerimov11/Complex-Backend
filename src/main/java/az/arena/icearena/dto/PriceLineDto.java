package az.arena.icearena.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceLineDto {
    private String label;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
}
