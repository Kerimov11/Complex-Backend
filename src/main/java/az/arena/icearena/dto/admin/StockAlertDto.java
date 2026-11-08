package az.arena.icearena.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlertDto {
    private Long skateId;
    private Integer size;
    private Integer availableQuantity;
    private Integer totalQuantity;
    private Integer damagedQuantity;
    private String level;
}
