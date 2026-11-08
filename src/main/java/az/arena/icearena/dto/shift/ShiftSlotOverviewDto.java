package az.arena.icearena.dto.shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSlotOverviewDto {
    private Long shiftSlotId;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;
    private int totalVolunteers;
    private int onDutyVolunteers;
    private int assignedVolunteers;
    private int unassignedVolunteers;
    private List<StationCountDto> stations;
    private List<VolunteerShiftDto> volunteers;
}
