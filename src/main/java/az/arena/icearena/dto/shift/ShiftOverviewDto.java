package az.arena.icearena.dto.shift;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftOverviewDto {
    private LocalDate date;
    private int totalVolunteers;
    private int onDutyVolunteers;
    private int assignedVolunteers;
    private int unassignedVolunteers;
    private List<StationCountDto> stationTotals;
    private List<ShiftSlotOverviewDto> slots;
}
