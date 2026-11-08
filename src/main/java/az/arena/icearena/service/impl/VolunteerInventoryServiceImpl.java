package az.arena.icearena.service.impl;

import az.arena.icearena.dto.volunteer.VolunteerInventoryDto;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.service.VolunteerInventoryService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VolunteerInventoryServiceImpl implements VolunteerInventoryService {

    private final SkateRepository skateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerInventoryDto> listInventory() {
        return skateRepository.findAll().stream()
                .sorted(Comparator.comparing(Skate::getSize))
                .map(this::toDto)
                .toList();
    }

    private VolunteerInventoryDto toDto(Skate skate) {
        int total = value(skate.getTotalQuantity());
        int available = value(skate.getAvailableQuantity());
        int inUse = Math.max(0, total - available);
        double percent = total == 0 ? 0d : Math.round((available * 10000d) / total) / 100d;
        return VolunteerInventoryDto.builder()
                .id(skate.getId())
                .size(skate.getSize())
                .totalQuantity(total)
                .availableQuantity(available)
                .inUseQuantity(inUse)
                .damagedQuantity(value(skate.getDamagedQuantity()))
                .availabilityPercent(percent)
                .build();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
