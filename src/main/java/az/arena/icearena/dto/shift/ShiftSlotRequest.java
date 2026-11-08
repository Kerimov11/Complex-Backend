package az.arena.icearena.dto.shift;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSlotRequest {

    @NotNull(message = "Tarix tələb olunur")
    private LocalDate shiftDate;

    @NotNull(message = "Başlama vaxtı tələb olunur")
    private LocalTime startTime;

    @NotNull(message = "Bitmə vaxtı tələb olunur")
    private LocalTime endTime;

    private Boolean active;
}
