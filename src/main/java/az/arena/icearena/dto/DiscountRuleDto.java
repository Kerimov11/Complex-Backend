package az.arena.icearena.dto;

import az.arena.icearena.entity.DiscountType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRuleDto {
    private Long id;
    private DiscountType type;
    private String title;
    private String description;
    private BigDecimal percent;
    private Integer minParticipants;
    private Integer freeTickets;
    private Integer durationMinutes;
    private Boolean active;
}
