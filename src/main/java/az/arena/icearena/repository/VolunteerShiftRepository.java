package az.arena.icearena.repository;

import az.arena.icearena.entity.VolunteerShift;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerShiftRepository extends JpaRepository<VolunteerShift, Long> {

    List<VolunteerShift> findByShiftSlotId(Long shiftSlotId);

    List<VolunteerShift> findByShiftSlotIdIn(Collection<Long> shiftSlotIds);

    List<VolunteerShift> findByShiftSlotShiftDateOrderByShiftSlotStartTimeAscUserLastNameAsc(
            LocalDate shiftDate);

    List<VolunteerShift> findByUserIdOrderByShiftSlotShiftDateDescShiftSlotStartTimeAsc(Long userId);

    Optional<VolunteerShift> findByUserIdAndShiftSlotId(Long userId, Long shiftSlotId);

    boolean existsByUserIdAndShiftSlotId(Long userId, Long shiftSlotId);

    long countByShiftSlotId(Long shiftSlotId);
}
