package az.arena.icearena.dto.shift;

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
public class ShiftSlotDto {
    private Long id;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;
    private long selectedCount;
    private Long myVolunteerShiftId;
    private String myStation;
    private String myStationLabel;
    private String myStatus;
}
