package az.arena.icearena.repository;

import az.arena.icearena.entity.ShiftSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftSlotRepository extends JpaRepository<ShiftSlot, Long> {

    List<ShiftSlot> findByShiftDateOrderByStartTimeAsc(LocalDate shiftDate);

    List<ShiftSlot> findByShiftDateAndActiveTrueOrderByStartTimeAsc(LocalDate shiftDate);

    List<ShiftSlot> findByShiftDateBetweenOrderByShiftDateAscStartTimeAsc(LocalDate from, LocalDate to);

    Optional<ShiftSlot> findByIdAndActiveTrue(Long id);

    boolean existsByShiftDateAndStartTimeAndEndTime(LocalDate shiftDate,
                                                    LocalTime startTime,
                                                    LocalTime endTime);

    boolean existsByShiftDateAndStartTimeAndEndTimeAndIdNot(LocalDate shiftDate,
                                                            LocalTime startTime,
                                                            LocalTime endTime,
                                                            Long id);
}
