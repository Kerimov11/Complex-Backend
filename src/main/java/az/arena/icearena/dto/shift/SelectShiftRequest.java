package az.arena.icearena.dto.shift;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectShiftRequest {

    @NotNull(message = "Smen slotu tələb olunur")
    private Long shiftSlotId;
}
