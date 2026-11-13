package az.arena.icearena.service.impl;

import az.arena.icearena.dto.admin.AdminDashboardDto;
import az.arena.icearena.dto.admin.RevenuePointDto;
import az.arena.icearena.dto.admin.SessionOccupancyDto;
import az.arena.icearena.dto.admin.StockAlertDto;
import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.PaymentChannel;
import az.arena.icearena.entity.PaymentStatus;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.repository.PaymentRepository;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.service.AdminDashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final SessionRepository sessionRepository;
    private final SkateRepository skateRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        return getDashboard(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Payment> payments = paymentRepository.findAll();
        List<Payment> todayPayments = payments.stream()
                .filter(payment -> within(payment, dayStart, dayEnd))
                .toList();
        List<Payment> successful = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .toList();
        List<Payment> todaySuccessful = todayPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .toList();

        BigDecimal todayOnline = sum(todaySuccessful.stream()
                .filter(payment -> payment.getChannel() != PaymentChannel.POS)
                .toList());
        BigDecimal todayPos = sum(todaySuccessful.stream()
                .filter(payment -> payment.getChannel() == PaymentChannel.POS)
                .toList());

        List<Reservation> todayReservations = reservationRepository.findByCreatedAtBetween(dayStart, dayEnd);
        List<Session> todaySessions = sessionRepository.findBySessionDate(date);
        LocalTime now = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());
        long activeSessions = todaySessions.stream()
                .filter(session -> !Boolean.TRUE.equals(session.getBlocked()))
                .filter(session -> isToday
                        ? (!now.isBefore(session.getStartTime()) && !now.isAfter(session.getEndTime()))
                        : true)
                .count();
        int capacity = todaySessions.stream()
                .mapToInt(session -> value(session.getMaxTicketLimit()))
                .sum();
        int occupied = todaySessions.stream()
                .mapToInt(session -> Math.max(0,
                        value(session.getMaxTicketLimit()) - value(session.getAvailableTickets())))
                .sum();

        long checkedInOnDate = reservationRepository.findAll().stream()
                .filter(r -> r.getCheckedInAt() != null && r.getCheckedInAt().toLocalDate().equals(date))
                .count();
        if (checkedInOnDate == 0 && isToday) {
            checkedInOnDate = reservationRepository.findByCheckedInAtIsNotNull().size()
                    + ticketRepository.countByIsUsedTrue();
        }

        return AdminDashboardDto.builder()
                .selectedDate(date)
                .todayRevenue(todayOnline.add(todayPos))
                .totalRevenue(sum(successful))
                .todayOnlineRevenue(todayOnline)
                .todayPosRevenue(todayPos)
                .todayTransactions(todayPayments.size())
                .totalPayments(payments.size())
                .successfulPayments(successful.size())
                .pendingPayments(todayPayments.stream()
                        .filter(payment -> payment.getStatus() == PaymentStatus.PENDING).count())
                .failedPayments(todayPayments.stream()
                        .filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count())
                .todayReservations(todayReservations.size())
                .confirmedReservations(todayReservations.stream()
                        .filter(reservation -> reservation.getStatus() == Reservation.Status.CONFIRMED)
                        .count())
                .todaySessions(todaySessions.size())
                .activeSessions(activeSessions)
                .checkedInTickets(checkedInOnDate)
                .todayOccupancyRate(capacity == 0 ? 0d : round(occupied * 100d / capacity))
                .sessions(buildSessions(todaySessions, now, isToday))
                .stockAlerts(buildStockAlerts())
                .revenueTrend(buildTrend(payments, date))
                .build();
    }

    private List<SessionOccupancyDto> buildSessions(List<Session> sessions, LocalTime now, boolean isToday) {
        return sessions.stream()
                .sorted(Comparator.comparing(Session::getStartTime))
                .map(session -> {
                    int max = value(session.getMaxTicketLimit());
                    int available = value(session.getAvailableTickets());
                    int occupied = Math.max(0, max - available);
                    boolean active = !Boolean.TRUE.equals(session.getBlocked())
                            && (!isToday || (!now.isBefore(session.getStartTime()) && !now.isAfter(session.getEndTime())));
                    return SessionOccupancyDto.builder()
                            .id(session.getId())
                            .startTime(session.getStartTime() == null ? "—" : session.getStartTime().format(TIME_FORMAT))
                            .endTime(session.getEndTime() == null ? "—" : session.getEndTime().format(TIME_FORMAT))
                            .start(session.getStartTime())
                            .end(session.getEndTime())
                            .maxTickets(max)
                            .availableTickets(available)
                            .occupiedTickets(occupied)
                            .occupancyRate(max == 0 ? 0d : round(occupied * 100d / max))
                            .blocked(Boolean.TRUE.equals(session.getBlocked()))
                            .active(active)
                            .build();
                })
                .toList();
    }

    private List<StockAlertDto> buildStockAlerts() {
        List<StockAlertDto> alerts = new ArrayList<>();
        List<Skate> skates = new ArrayList<>(skateRepository.findAll());
        skates.sort(Comparator.comparing(Skate::getSize));
        for (Skate skate : skates) {
            int total = value(skate.getTotalQuantity());
            int available = value(skate.getAvailableQuantity());
            int damaged = value(skate.getDamagedQuantity());
            double availableRate = total == 0 ? 0 : available * 100d / total;
            if (available <= 5 || availableRate < 20d || damaged > 0) {
                String level = available == 0 ? "CRITICAL"
                        : (available <= 5 || availableRate < 20d) ? "LOW" : "DAMAGED";
                alerts.add(StockAlertDto.builder()
                        .skateId(skate.getId())
                        .size(skate.getDisplaySize())
                        .availableQuantity(available)
                        .totalQuantity(total)
                        .damagedQuantity(damaged)
                        .level(level)
                        .build());
            }
        }
        return alerts;
    }

    private List<RevenuePointDto> buildTrend(List<Payment> payments, LocalDate baseDate) {
        List<RevenuePointDto> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = baseDate.minusDays(offset);
            List<Payment> dayPayments = payments.stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                    .filter(payment -> payment.getPaymentDate() != null
                            && payment.getPaymentDate().toLocalDate().equals(date))
                    .toList();
            trend.add(RevenuePointDto.builder()
                    .date(date)
                    .amount(sum(dayPayments))
                    .transactions(dayPayments.size())
                    .build());
        }
        return trend;
    }

    private boolean within(Payment payment, LocalDateTime start, LocalDateTime end) {
        return payment.getPaymentDate() != null
                && !payment.getPaymentDate().isBefore(start)
                && !payment.getPaymentDate().isAfter(end);
    }

    private BigDecimal sum(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
