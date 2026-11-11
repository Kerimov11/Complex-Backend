package az.arena.icearena.mapper;

import az.arena.icearena.dto.shift.StationCountDto;
import az.arena.icearena.dto.shift.VolunteerShiftDto;
import az.arena.icearena.entity.ShiftAssignmentStatus;
import az.arena.icearena.entity.Station;
import az.arena.icearena.entity.User;
import az.arena.icearena.entity.VolunteerShift;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShiftMapper {

    public VolunteerShiftDto toDto(VolunteerShift shift, LocalDateTime now) {
        User user = shift.getUser();
        LocalDate shiftDate = shift.getShiftSlot() != null
                ? shift.getShiftSlot().getShiftDate()
                : shift.getCancelledShiftDate();
        LocalTime startTime = shift.getShiftSlot() != null
                ? shift.getShiftSlot().getStartTime()
                : shift.getCancelledStartTime();
        LocalTime endTime = shift.getShiftSlot() != null
                ? shift.getShiftSlot().getEndTime()
                : shift.getCancelledEndTime();
        return VolunteerShiftDto.builder()
                .id(shift.getId())
                .userId(user == null ? null : user.getId())
                .username(user == null ? null : user.getUsername())
                .fullName(fullName(user))
                .shiftSlotId(shift.getShiftSlot() == null ? null : shift.getShiftSlot().getId())
                .shiftDate(shiftDate)
                .startTime(startTime)
                .endTime(endTime)
                .station(shift.getStation())
                .stationLabel(shift.getStation() == null ? null : shift.getStation().getLabel())
                .status(shift.getStatus())
                .liveStatus(liveStatus(shift, now))
                .selectedAt(shift.getSelectedAt())
                .assignedAt(shift.getAssignedAt())
                .assignedBy(shift.getAssignedBy())
                .cancelledAt(shift.getCancelledAt())
                .cancellationMessage(shift.getCancellationMessage())
                .build();
    }

    public String liveStatus(VolunteerShift shift, LocalDateTime now) {
        if (shift == null) {
            return "UNKNOWN";
        }
        if (shift.getStatus() == ShiftAssignmentStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (shift.getShiftSlot() == null) {
            return "UNKNOWN";
        }
        LocalDate date = shift.getShiftSlot().getShiftDate();
        LocalTime start = shift.getShiftSlot().getStartTime();
        LocalTime end = shift.getShiftSlot().getEndTime();
        if (date == null || start == null || end == null) {
            return "UNKNOWN";
        }
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        if (date.isBefore(today) || (date.equals(today) && end.isBefore(time))) {
            return "COMPLETED";
        }
        if (date.isAfter(today) || (date.equals(today) && start.isAfter(time))) {
            return "UPCOMING";
        }
        return "ON_DUTY";
    }

    public List<StationCountDto> stationCounts(Collection<VolunteerShift> shifts) {
        List<StationCountDto> counts = new ArrayList<>();
        for (Station station : Station.values()) {
            long count = shifts.stream()
                    .filter(shift -> shift.getStation() == station)
                    .count();
            counts.add(StationCountDto.builder()
                    .station(station)
                    .label(station.getLabel())
                    .count(count)
                    .build());
        }
        return counts;
    }

    private String fullName(User user) {
        if (user == null) {
            return "Naməlum könüllü";
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName())
                + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }
}
