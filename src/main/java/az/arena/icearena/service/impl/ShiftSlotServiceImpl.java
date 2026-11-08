package az.arena.icearena.service.impl;

import az.arena.icearena.dto.shift.ShiftSlotDto;
import az.arena.icearena.dto.shift.ShiftSlotRequest;
import az.arena.icearena.entity.ShiftSlot;
import az.arena.icearena.entity.User;
import az.arena.icearena.entity.VolunteerShift;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ShiftSlotRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.repository.VolunteerShiftRepository;
import az.arena.icearena.service.ShiftSlotService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftSlotServiceImpl implements ShiftSlotService {

    private final ShiftSlotRepository shiftSlotRepository;
    private final VolunteerShiftRepository volunteerShiftRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShiftSlotDto> listAvailable(LocalDate date, String username) {
        LocalDate targetDate = targetDate(date);
        List<ShiftSlot> slots = shiftSlotRepository
                .findByShiftDateAndActiveTrueOrderByStartTimeAsc(targetDate);
        List<VolunteerShift> selected = volunteerShiftRepository
                .findByShiftSlotShiftDateOrderByShiftSlotStartTimeAscUserLastNameAsc(targetDate);

        Map<Long, List<VolunteerShift>> bySlot = selected.stream()
                .collect(Collectors.groupingBy(shift -> shift.getShiftSlot().getId()));
        User user = username == null ? null : userRepository.findByUsername(username).orElse(null);
        Map<Long, VolunteerShift> mine = user == null
                ? Map.of()
                : selected.stream()
                .filter(shift -> shift.getUser() != null
                        && shift.getUser().getId().equals(user.getId()))
                .collect(Collectors.toMap(shift -> shift.getShiftSlot().getId(),
                        Function.identity(), (left, right) -> left));

        return slots.stream()
                .map(slot -> toDto(slot, bySlot.getOrDefault(slot.getId(), List.of()).size(),
                        mine.get(slot.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftSlotDto> listAll(LocalDate date) {
        LocalDate targetDate = targetDate(date);
        List<ShiftSlot> slots = shiftSlotRepository
                .findByShiftDateOrderByStartTimeAsc(targetDate);
        List<VolunteerShift> selected = volunteerShiftRepository
                .findByShiftSlotShiftDateOrderByShiftSlotStartTimeAscUserLastNameAsc(targetDate);
        Map<Long, Long> counts = selected.stream()
                .collect(Collectors.groupingBy(
                        shift -> shift.getShiftSlot().getId(),
                        Collectors.counting()));
        return slots.stream()
                .map(slot -> toDto(slot, counts.getOrDefault(slot.getId(), 0L), null))
                .toList();
    }

    @Override
    @Transactional
    public ShiftSlotDto create(ShiftSlotRequest request) {
        validate(request);
        if (shiftSlotRepository.existsByShiftDateAndStartTimeAndEndTime(
                request.getShiftDate(), request.getStartTime(), request.getEndTime())) {
            throw new BadRequestException("Bu tarix və saat aralığı üçün smen artıq mövcuddur.");
        }
        LocalDateTime now = LocalDateTime.now();
        ShiftSlot slot = ShiftSlot.builder()
                .shiftDate(request.getShiftDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .active(request.getActive() == null || request.getActive())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toDto(shiftSlotRepository.save(slot), 0L, null);
    }

    @Override
    @Transactional
    public ShiftSlotDto update(Long id, ShiftSlotRequest request) {
        validate(request);
        ShiftSlot slot = findSlot(id);
        if (shiftSlotRepository.existsByShiftDateAndStartTimeAndEndTimeAndIdNot(
                request.getShiftDate(), request.getStartTime(), request.getEndTime(), id)) {
            throw new BadRequestException("Bu tarix və saat aralığı üçün smen artıq mövcuddur.");
        }
        slot.setShiftDate(request.getShiftDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        if (request.getActive() != null) {
            slot.setActive(request.getActive());
        }
        slot.setUpdatedAt(LocalDateTime.now());
        long selectedCount = volunteerShiftRepository.countByShiftSlotId(id);
        return toDto(shiftSlotRepository.save(slot), selectedCount, null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ShiftSlot slot = findSlot(id);
        long selectedCount = volunteerShiftRepository.countByShiftSlotId(id);
        if (selectedCount > 0) {
            throw new BadRequestException(
                    "Bu smenə " + selectedCount + " könüllü seçilib. Əvvəlcə onları başqa smenə keçirin.");
        }
        shiftSlotRepository.delete(slot);
    }

    private ShiftSlot findSlot(Long id) {
        return shiftSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Smen slotu tapılmadı: " + id));
    }

    private void validate(ShiftSlotRequest request) {
        if (request == null || request.getShiftDate() == null
                || request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Tarix, başlama və bitmə vaxtı tələb olunur.");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Bitmə vaxtı başlama vaxtından sonra olmalıdır.");
        }
    }

    private LocalDate targetDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private ShiftSlotDto toDto(ShiftSlot slot, long selectedCount, VolunteerShift mine) {
        return ShiftSlotDto.builder()
                .id(slot.getId())
                .shiftDate(slot.getShiftDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .active(Boolean.TRUE.equals(slot.getActive()))
                .selectedCount(selectedCount)
                .myVolunteerShiftId(mine == null ? null : mine.getId())
                .myStation(mine == null || mine.getStation() == null ? null : mine.getStation().name())
                .myStationLabel(mine == null || mine.getStation() == null
                        ? null : mine.getStation().getLabel())
                .myStatus(mine == null ? null : mine.getStatus().name())
                .build();
    }
}
