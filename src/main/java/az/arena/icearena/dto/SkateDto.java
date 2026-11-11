package az.arena.icearena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkateDto {
    private Long id;
    private String size;
    private Integer sortKey;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer damagedQuantity;
}
