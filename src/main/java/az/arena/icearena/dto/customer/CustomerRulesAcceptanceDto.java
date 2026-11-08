package az.arena.icearena.dto.customer;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRulesAcceptanceDto {

    private Set<String> acceptedObligationIds;
    private int acceptedCount;
    private int totalCount;
    private boolean confirmed;
    private LocalDateTime updatedAt;
}
