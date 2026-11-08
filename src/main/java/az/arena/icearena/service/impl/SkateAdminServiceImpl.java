package az.arena.icearena.service.impl;

import az.arena.icearena.dto.SkateDto;
import az.arena.icearena.dto.admin.SkateStockRequest;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.service.SkateAdminService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkateAdminServiceImpl implements SkateAdminService {

    private final SkateRepository skateRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SkateDto> list() {
        return skateRepository.findAll().stream()
                .sorted(Comparator.comparing(Skate::getSize))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SkateDto create(SkateStockRequest request) {
        if (request.getSize() == null || request.getSize() <= 0) {
            throw new BadRequestException("Konki ölçüsü düzgün deyil.");
        }
        if (skateRepository.findBySize(request.getSize()).isPresent()) {
            throw new BadRequestException("Bu ölçü artıq mövcuddur.");
        }
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        Skate skate = Skate.builder()
                .size(request.getSize())
                .totalQuantity(quantity)
                .availableQuantity(quantity)
                .damagedQuantity(0)
                .build();
        return toDto(skateRepository.save(skate));
    }

    @Override
    @Transactional
    public SkateDto restock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Əlavə olunacaq say 0-dan böyük olmalıdır.");
        }
        Skate skate = findSkate(id);
        skate.setTotalQuantity(value(skate.getTotalQuantity()) + quantity);
        skate.setAvailableQuantity(value(skate.getAvailableQuantity()) + quantity);
        return toDto(skateRepository.save(skate));
    }

    @Override
    @Transactional
    public SkateDto writeOff(Long id, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Silinəcək say 0-dan böyük olmalıdır.");
        }
        Skate skate = findSkate(id);
        int available = value(skate.getAvailableQuantity());
        int total = value(skate.getTotalQuantity());
        if (available < quantity) {
            throw new BadRequestException(
                    "Zədələnmiş inventar kimi silmək üçün boşda olan konki sayı kifayət etmir.");
        }
        if (total < quantity) {
            throw new BadRequestException("Ümumi inventar sayı kifayət etmir.");
        }
        skate.setAvailableQuantity(available - quantity);
        skate.setTotalQuantity(total - quantity);
        skate.setDamagedQuantity(value(skate.getDamagedQuantity()) + quantity);
        return toDto(skateRepository.save(skate));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Skate skate = findSkate(id);
        if (value(skate.getTotalQuantity()) > 0) {
            throw new BadRequestException(
                    "Stokda konki qalıb. Əvvəlcə zədəli kimi silin və ya sayı sıfıra endirin.");
        }
        if (reservationRepository.existsBySkateId(id)) {
            throw new BadRequestException(
                    "Bu ölçüyə bağlı rezervasiyalar var; inventar qeydini silmək mümkün deyil.");
        }
        skateRepository.delete(skate);
    }

    private Skate findSkate(Long id) {
        return skateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Konki tapılmadı: " + id));
    }

    private SkateDto toDto(Skate skate) {
        return SkateDto.builder()
                .id(skate.getId())
                .size(skate.getSize())
                .totalQuantity(value(skate.getTotalQuantity()))
                .availableQuantity(value(skate.getAvailableQuantity()))
                .damagedQuantity(value(skate.getDamagedQuantity()))
                .build();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
