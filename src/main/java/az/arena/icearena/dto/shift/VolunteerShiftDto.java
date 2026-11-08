package az.arena.icearena.dto.shift;

import az.arena.icearena.entity.ShiftAssignmentStatus;
import az.arena.icearena.entity.Station;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerShiftDto {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private Long shiftSlotId;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Station station;
    private String stationLabel;
    private ShiftAssignmentStatus status;
    private String liveStatus;
    private LocalDateTime selectedAt;
    private LocalDateTime assignedAt;
    private String assignedBy;
}
