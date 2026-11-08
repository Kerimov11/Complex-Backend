package az.arena.icearena.service;

import az.arena.icearena.dto.volunteer.VolunteerInventoryDto;
import java.util.List;

public interface VolunteerInventoryService {
    List<VolunteerInventoryDto> listInventory();
}
