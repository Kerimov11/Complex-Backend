package az.arena.icearena.service;

import az.arena.icearena.dto.SkateDto;
import az.arena.icearena.dto.admin.SkateStockRequest;
import java.util.List;

public interface SkateAdminService {
    List<SkateDto> list();

    SkateDto create(SkateStockRequest request);

    SkateDto updateStock(Long id, SkateStockRequest request);

    SkateDto restock(Long id, int quantity);

    SkateDto writeOff(Long id, int quantity);

    void delete(Long id);
}
