package az.arena.icearena.service;

import az.arena.icearena.dto.admin.PosSaleRequest;
import az.arena.icearena.dto.admin.PosSaleResponse;
import az.arena.icearena.dto.admin.CashierReportDto;
import az.arena.icearena.dto.PaymentDto;
import java.time.LocalDate;
import java.util.List;

public interface CashierPosService {
    PosSaleResponse sell(PosSaleRequest request, String cashierUsername);

    CashierReportDto report(LocalDate date, String username, boolean allCashiers);

    List<PaymentDto> sales(LocalDate date, String username, boolean allCashiers);
}
