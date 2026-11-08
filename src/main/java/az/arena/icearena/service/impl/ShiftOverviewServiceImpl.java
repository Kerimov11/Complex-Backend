package az.arena.icearena.service.impl;

import az.arena.icearena.dto.shift.ShiftOverviewDto;
import az.arena.icearena.dto.shift.ShiftSlotOverviewDto;
import az.arena.icearena.dto.shift.VolunteerShiftDto;
import az.arena.icearena.entity.ShiftSlot;
import az.arena.icearena.entity.VolunteerShift;
import az.arena.icearena.mapper.ShiftMapper;
import az.arena.icearena.repository.ShiftSlotRepository;
import az.arena.icearena.repository.VolunteerShiftRepository;
import az.arena.icearena.service.ShiftOverviewService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftOverviewServiceImpl implements ShiftOverviewService {

    private final ShiftSlotRepository shiftSlotRepository;
    private final VolunteerShiftRepository volunteerShiftRepository;
    private final ShiftMapper shiftMapper;

    @Override
    @Transactional(readOnly = true)
    public ShiftOverviewDto overview(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime now = LocalDateTime.now();
        List<ShiftSlot> slots = shiftSlotRepository
                .findByShiftDateOrderByStartTimeAsc(targetDate);
        List<VolunteerShift> allShifts = volunteerShiftRepository
                .findByShiftSlotShiftDateOrderByShiftSlotStartTimeAscUserLastNameAsc(targetDate);
        Map<Long, List<VolunteerShift>> bySlot = allShifts.stream()
                .collect(Collectors.groupingBy(shift -> shift.getShiftSlot().getId()));

        List<ShiftSlotOverviewDto> slotDtos = new ArrayList<>();
        for (ShiftSlot slot : slots) {
            List<VolunteerShift> slotShifts = bySlot.getOrDefault(slot.getId(), List.of());
            List<VolunteerShiftDto> volunteers = slotShifts.stream()
                    .map(shift -> shiftMapper.toDto(shift, now))
                    .toList();
            int assigned = (int) slotShifts.stream()
                    .filter(shift -> shift.getStation() != null)
                    .count();
            int onDuty = (int) slotShifts.stream()
                    .filter(shift -> "ON_DUTY".equals(shiftMapper.liveStatus(shift, now)))
                    .count();
            slotDtos.add(ShiftSlotOverviewDto.builder()
                    .shiftSlotId(slot.getId())
                    .shiftDate(slot.getShiftDate())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .active(Boolean.TRUE.equals(slot.getActive()))
                    .totalVolunteers(slotShifts.size())
                    .onDutyVolunteers(onDuty)
                    .assignedVolunteers(assigned)
                    .unassignedVolunteers(slotShifts.size() - assigned)
                    .stations(shiftMapper.stationCounts(slotShifts))
                    .volunteers(volunteers)
                    .build());
        }

        int total = allShifts.size();
        int assigned = (int) allShifts.stream()
                .filter(shift -> shift.getStation() != null)
                .count();
        int onDuty = (int) allShifts.stream()
                .filter(shift -> "ON_DUTY".equals(shiftMapper.liveStatus(shift, now)))
                .count();

        return ShiftOverviewDto.builder()
                .date(targetDate)
                .totalVolunteers(total)
                .onDutyVolunteers(onDuty)
                .assignedVolunteers(assigned)
                .unassignedVolunteers(total - assigned)
                .stationTotals(shiftMapper.stationCounts(allShifts))
                .slots(slotDtos)
                .build();
    }
}
