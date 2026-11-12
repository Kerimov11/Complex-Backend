package az.arena.icearena.service.impl;

import az.arena.icearena.dto.BookingPaymentRequest;
import az.arena.icearena.dto.DiscountBreakdownDto;
import az.arena.icearena.dto.DiscountRequest;
import az.arena.icearena.dto.PaymentDto;
import az.arena.icearena.dto.SessionAvailabilityDto;
import az.arena.icearena.dto.admin.CashierReportDto;
import az.arena.icearena.dto.admin.PosSaleRequest;
import az.arena.icearena.dto.admin.PosSaleResponse;
import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.PaymentChannel;
import az.arena.icearena.entity.PaymentMethod;
import az.arena.icearena.entity.PaymentStatus;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Ticket;
import az.arena.icearena.entity.TicketType;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.mapper.PaymentMapper;
import az.arena.icearena.repository.PaymentRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CashierPosService;
import az.arena.icearena.service.DiscountService;
import az.arena.icearena.service.SessionTimeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashierPosServiceImpl implements CashierPosService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final DiscountService discountService;
    private final SessionTimeService sessionTimeService;

    @Override
    @Transactional
    public PosSaleResponse sell(PosSaleRequest request, String cashierUsername) {
        if (request.getPaymentMethod() != PaymentMethod.CASH
                && request.getPaymentMethod() != PaymentMethod.CARD) {
            throw new BadRequestException("POS satışında ödəniş üsulu nağd və ya kart olmalıdır.");
        }
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        if (quantity <= 0) {
            throw new BadRequestException("Bilet sayı 0-dan böyük olmalıdır.");
        }

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Kassir tapılmadı."));

        LocalDateTime now = LocalDateTime.now();
        Session session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId()).orElse(null);
        }

        List<Session> todaySessions = sessionRepository.findBySessionDate(now.toLocalDate());
        LocalTime currentTime = now.toLocalTime();

        if (session == null || Boolean.TRUE.equals(session.getBlocked())) {
            // 1. Cari aktiv sessiya (başlama və bitmə saatı aralığında)
            session = todaySessions.stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getBlocked()))
                    .filter(s -> s.getAvailableTickets() != null && s.getAvailableTickets() >= quantity)
                    .filter(s -> !currentTime.isBefore(s.getStartTime()) && !currentTime.isAfter(s.getEndTime()))
                    .findFirst()
                    .orElse(null);

            // 2. Qarşıdakı növbəti sessiya
            if (session == null) {
                session = todaySessions.stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getBlocked()))
                        .filter(s -> s.getAvailableTickets() != null && s.getAvailableTickets() >= quantity)
                        .filter(s -> currentTime.isBefore(s.getStartTime()))
                        .min(Comparator.comparing(Session::getStartTime))
                        .orElse(null);
            }

            // 3. Ən sonuncu mövcud sessiya
            if (session == null) {
                session = todaySessions.stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getBlocked()))
                        .filter(s -> s.getAvailableTickets() != null && s.getAvailableTickets() >= quantity)
                        .max(Comparator.comparing(Session::getStartTime))
                        .orElse(null);
            }

            // 4. Günün istənilən sessiyası
            if (session == null && !todaySessions.isEmpty()) {
                session = todaySessions.get(todaySessions.size() - 1);
            }
        }

        if (session == null) {
            throw new BadRequestException("Hazırda aktiv və ya uyğun açıq sessiya tapılmadı.");
        }

        if (Boolean.TRUE.equals(session.getBlocked())) {
            throw new BadRequestException(session.getBlockReason() == null
                    ? "Bu sessiya bloklanıb."
                    : "Sessiya bloklanıb: " + session.getBlockReason());
        }
        int available = session.getAvailableTickets() == null ? 0 : session.getAvailableTickets();
        if (available < quantity) {
            throw new BadRequestException("Bu sessiyada kifayət qədər bilet yoxdur. Qalıq: " + available);
        }

        int duration = request.getDurationMinutes() == null ? 60 : request.getDurationMinutes();
        LocalTime entry = resolveEntryTime(session, now);

        DiscountBreakdownDto pricing = discountService.calculate(DiscountRequest.builder()
                .sessionId(session.getId())
                .date(session.getSessionDate())
                .startTime(entry)
                .durationMinutes(duration)
                .quantity(quantity)
                .build());
        BigDecimal total = pricing.getFinalPrice();
        if (total == null || total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Satış məbləği hesablanmadı.");
        }
        BigDecimal unitPrice = total.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);

        String reference = "POS-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10).toUpperCase();

        List<String> names = request.getCustomerNames();
        String primaryCustomerName = trimToNull(request.getCustomerName());
        if (primaryCustomerName == null && names != null && !names.isEmpty()) {
            primaryCustomerName = trimToNull(names.get(0));
        }
        String customerPhone = trimToNull(request.getCustomerPhone());

        List<PosSaleResponse.PosTicketDto> soldTickets = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String ticketPersonName = null;
            if (names != null && i < names.size()) {
                ticketPersonName = trimToNull(names.get(i));
            }
            if (ticketPersonName == null) {
                ticketPersonName = primaryCustomerName;
            }

            String code = "POS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            Ticket ticket = Ticket.builder()
                    .ticketCode(code)
                    .qrCode("QR-" + code)
                    .ticketType(TicketType.BIG)
                    .price(unitPrice)
                    .purchaseDate(now)
                    .isUsed(false)
                    .paymentMethod(request.getPaymentMethod())
                    .soldByUsername(cashierUsername)
                    .durationMinutes(duration)
                    .entryTime(entry)
                    .customerName(ticketPersonName)
                    .customerPhone(customerPhone)
                    .saleReference(reference)
                    .user(cashier)
                    .session(session)
                    .build();
            Ticket saved = ticketRepository.save(ticket);
            soldTickets.add(PosSaleResponse.PosTicketDto.builder()
                    .id(saved.getId())
                    .ticketCode(saved.getTicketCode())
                    .qrCode(saved.getQrCode())
                    .price(saved.getPrice())
                    .customerName(saved.getCustomerName())
                    .build());
        }

        session.setAvailableTickets(available - quantity);
        sessionRepository.save(session);

        String paymentCustomerName = primaryCustomerName;
        if (names != null && !names.isEmpty()) {
            List<String> filteredNames = names.stream()
                    .map(this::trimToNull)
                    .filter(n -> n != null)
                    .toList();
            if (!filteredNames.isEmpty()) {
                paymentCustomerName = String.join(", ", filteredNames);
            }
        }

        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID().toString())
                .reference(reference)
                .amount(total)
                .status(PaymentStatus.SUCCESS)
                .paymentDate(now)
                .completionDate(now)
                .channel(PaymentChannel.POS)
                .paymentMethod(request.getPaymentMethod())
                .cashierUsername(cashierUsername)
                .customerName(paymentCustomerName)
                .quantity(quantity)
                .user(cashier)
                .build();
        paymentRepository.save(payment);

        return PosSaleResponse.builder()
                .saleReference(reference)
                .totalAmount(total)
                .unitPrice(unitPrice)
                .paymentMethod(request.getPaymentMethod())
                .cashierUsername(cashierUsername)
                .customerName(payment.getCustomerName())
                .sessionId(session.getId())
                .soldAt(now)
                .quantity(quantity)
                .tickets(soldTickets)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CashierReportDto report(LocalDate date, String username, boolean allCashiers) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<Payment> payments = posPayments(target, username, allCashiers);

        BigDecimal cash = sumByMethod(payments, PaymentMethod.CASH);
        BigDecimal card = sumByMethod(payments, PaymentMethod.CARD);
        int tickets = payments.stream()
                .map(Payment::getQuantity)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        return CashierReportDto.builder()
                .date(target)
                .cashierUsername(allCashiers ? null : username)
                .cashTotal(cash)
                .cardTotal(card)
                .totalAmount(cash.add(card))
                .transactionCount(payments.size())
                .ticketCount(tickets)
                .sales(payments.stream()
                        .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                        .map(paymentMapper::toDto)
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> sales(LocalDate date, String username, boolean allCashiers) {
        return posPayments(date == null ? LocalDate.now() : date, username, allCashiers)
                .stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .map(paymentMapper::toDto)
                .toList();
    }

    private List<Payment> posPayments(LocalDate date, String username, boolean allCashiers) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        if (allCashiers) {
            return paymentRepository.findByChannelAndPaymentDateBetween(
                    PaymentChannel.POS, start, end);
        }
        return paymentRepository.findByCashierUsernameAndPaymentDateBetween(
                username, start, end).stream()
                .filter(payment -> payment.getChannel() == PaymentChannel.POS)
                .toList();
    }

    private BigDecimal sumByMethod(List<Payment> payments, PaymentMethod method) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .filter(payment -> payment.getPaymentMethod() == method)
                .map(Payment::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalTime resolveEntryTime(Session session, LocalDateTime now) {
        if (session.getSessionDate() == null) {
            return session.getStartTime();
        }
        if (session.getSessionDate().equals(now.toLocalDate())) {
            if (now.toLocalTime().isBefore(session.getStartTime())) {
                return session.getStartTime();
            }
            if (now.toLocalTime().isAfter(session.getEndTime())) {
                return session.getEndTime();
            }
            return now.toLocalTime();
        }
        return session.getStartTime();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
