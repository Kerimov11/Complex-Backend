package az.arena.icearena.dto.volunteer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerInventoryDto {
    private Long id;
    private Integer size;
    private int totalQuantity;
    private int availableQuantity;
    private int inUseQuantity;
    private int damagedQuantity;
    private double availabilityPercent;
}
