package az.arena.icearena.service;

import az.arena.icearena.dto.shift.ShiftSlotDto;
import az.arena.icearena.dto.shift.ShiftSlotRequest;
import java.time.LocalDate;
import java.util.List;

public interface ShiftSlotService {
    List<ShiftSlotDto> listAvailable(LocalDate date, String username);

    List<ShiftSlotDto> listAll(LocalDate date);

    ShiftSlotDto create(ShiftSlotRequest request);

    ShiftSlotDto update(Long id, ShiftSlotRequest request);

    void delete(Long id);
}
