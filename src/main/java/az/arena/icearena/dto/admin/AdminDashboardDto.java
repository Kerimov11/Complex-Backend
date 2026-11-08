package az.arena.icearena.dto.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDto {
    private BigDecimal todayRevenue;
    private BigDecimal totalRevenue;
    private BigDecimal todayOnlineRevenue;
    private BigDecimal todayPosRevenue;
    private long todayTransactions;
    private long totalPayments;
    private long successfulPayments;
    private long pendingPayments;
    private long failedPayments;
    private long todayReservations;
    private long confirmedReservations;
    private long todaySessions;
    private long activeSessions;
    private long checkedInTickets;
    private double todayOccupancyRate;
    private List<SessionOccupancyDto> sessions;
    private List<StockAlertDto> stockAlerts;
    private List<RevenuePointDto> revenueTrend;
}
