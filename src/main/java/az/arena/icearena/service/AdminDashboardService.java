package az.arena.icearena.service;

import az.arena.icearena.dto.admin.AdminDashboardDto;
import java.time.LocalDate;

public interface AdminDashboardService {
    AdminDashboardDto getDashboard();

    AdminDashboardDto getDashboard(LocalDate date);
}
