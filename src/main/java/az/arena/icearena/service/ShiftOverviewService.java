package az.arena.icearena.service;

import az.arena.icearena.dto.shift.ShiftOverviewDto;
import java.time.LocalDate;

public interface ShiftOverviewService {
    ShiftOverviewDto overview(LocalDate date);
}
