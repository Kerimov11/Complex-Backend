package az.arena.icearena.service.impl;

import az.arena.icearena.dto.BookingPaymentRequest;
import az.arena.icearena.dto.DiscountBreakdownDto;
import az.arena.icearena.dto.DiscountRequest;
import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.PaymentStatus;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.PaymentRepository;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SkateRentalRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.DiscountService;
import az.arena.icearena.service.PaymentService;
import az.arena.icearena.service.ReservationService;
import az.arena.icearena.service.CustomerVerificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final SkateRentalRepository skateRentalRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final DiscountService discountService;
    private final CustomerVerificationService customerVerificationService;

    @Override
    @Transactional
    public Payment createPaymentForTicket(Long ticketId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID().toString())
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .ticket(ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found")))
                .user(ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found")).getUser())
                .build();
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment createPaymentForSkateRental(Long skateRentalId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID().toString())
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .skateRental(skateRentalRepository.findById(skateRentalId)
                        .orElseThrow(() -> new ResourceNotFoundException("SkateRental not found")))
                .user(skateRentalRepository.findById(skateRentalId)
                        .orElseThrow(() -> new ResourceNotFoundException("SkateRental not found")).getUser())
                .build();
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment createBookingPayment(String username, BigDecimal amount, String reference) {
        return createBookingPayment(username, BookingPaymentRequest.builder()
                .amount(amount)
                .reference(reference)
                .build());
    }

    @Override
    @Transactional
    public Payment createBookingPayment(String username, BookingPaymentRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        customerVerificationService.requireTicketPurchaseEligibility(user);

        if (Boolean.TRUE.equals(request.getPush30())) {
            String normalizedPhone = request.getPush30Phone() == null
                    ? ""
                    : request.getPush30Phone().replaceAll("[^0-9]", "");
            if (normalizedPhone.length() < 9) {
                throw new BadRequestException("PUSH30 qeydiyyat telefon nömrəsini düzgün daxil edin.");
            }
            if (reservationRepository.existsByPush30PhoneAndStatusNot(
                    normalizedPhone, Reservation.Status.CANCELLED)) {
                throw new BadRequestException(
                        "Bu nömrə ilə PUSH30 endirimindən artıq istifadə olunub. PUSH30 endirimi yalnız 1 gedişlikdir.");
            }
        }

        if (Boolean.TRUE.equals(request.getQazi())) {
            String normalizedQazi = normalizeFullName(request.getQaziFullName());
            if (normalizedQazi.isEmpty()) {
                throw new BadRequestException("Qazi endirimi üçün ad və soyad tələb olunur.");
            }
            long usedVisits = reservationRepository.countDistinctQaziVisits(
                    normalizedQazi, Reservation.Status.CANCELLED);
            if (usedVisits >= 2) {
                throw new BadRequestException(
                        "Bu ad və soyadla Qazi endirimindən artıq 2 dəfə istifadə olunub.");
            }
            request.setQaziVisits((int) usedVisits);
        }

        List<Long> reservationIds = request.getReservationIds() == null
                ? List.of()
                : request.getReservationIds().stream().filter(id -> id != null).toList();

        BigDecimal amount = request.getAmount();
        if (!reservationIds.isEmpty()) {
            amount = verifyServerPrice(user, request, reservationIds);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Ödəniş məbləği 0-dan böyük olmalıdır.");
        }

        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID().toString())
                .reference(request.getReference())
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .reservationIds(reservationIds.isEmpty()
                        ? null
                        : reservationIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .user(user)
                .build();
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment processPayment(String transactionId, PaymentStatus status) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transactionId: " + transactionId));
        applyStatus(payment, status);
        Payment saved = paymentRepository.save(payment);
        syncReservations(payment, status);
        return saved;
    }

    @Override
    @Transactional
    public Payment processOwnPayment(String transactionId, String username, PaymentStatus status) {
        Payment payment = getOwnedPayment(transactionId, username);
        if (payment.getStatus() == status) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Bu ödəniş artıq emal olunub.");
        }
        applyStatus(payment, status);
        Payment saved = paymentRepository.save(payment);
        syncReservations(payment, status);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transactionId: " + transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentForUser(String transactionId, String username) {
        return getOwnedPayment(transactionId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPayments(PaymentStatus status) {
        return status == null
                ? paymentRepository.findAll()
                : paymentRepository.findByStatus(status);
    }

    /**
     * Frontend-in göndərdiyi məbləği kor-koranə qəbul etmirik.
     * Rezervasiyalardan tarix, müddət və say götürülür, endirim mühərriki
     * yenidən işlədilir və nəticə client məbləği ilə müqayisə olunur.
     */
    private BigDecimal verifyServerPrice(User user,
                                         BookingPaymentRequest request,
                                         List<Long> reservationIds) {
        List<Reservation> reservations = new ArrayList<>();
        for (Long id : reservationIds) {
            Reservation reservation = reservationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Rezervasiya tapılmadı: " + id));
            if (reservation.getUser() == null
                    || !user.getId().equals(reservation.getUser().getId())) {
                throw new BadRequestException("Rezervasiya bu istifadəçiyə aid deyil: " + id);
            }
            reservations.add(reservation);
        }

        Reservation first = reservations.get(0);
        if (first.getSession() == null) {
            throw new BadRequestException("Rezervasiyanın sessiyası tapılmadı.");
        }

        LocalTime startTime = request.getStartTime() != null
                ? request.getStartTime()
                : (first.getEntryTime() != null ? first.getEntryTime() : first.getSession().getStartTime());
        int duration = request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : (first.getDurationMinutes() != null ? first.getDurationMinutes() : 60);

        DiscountRequest pricingRequest = DiscountRequest.builder()
                .sessionId(first.getSession().getId())
                .date(first.getSession().getSessionDate())
                .startTime(startTime)
                .durationMinutes(duration)
                .quantity(reservations.size())
                .student(request.getStudent())
                .studentCount(request.getStudentCount())
                .family(request.getFamily())
                .push30(request.getPush30())
                .qazi(request.getQazi())
                .qaziVisits(request.getQaziVisits())
                .shaheed(request.getShaheed())
                .shaheedCount(request.getShaheedCount())
                .shaheedVerified(request.getShaheedVerified())
                .build();

        DiscountBreakdownDto discountResult = discountService.calculate(pricingRequest);
        BigDecimal expected = discountResult.getFinalPrice();
        if (request.getAmount() == null || request.getAmount().compareTo(expected) != 0) {
            throw new BadRequestException(
                    "Ödəniş məbləği server hesablaması ilə uyğun deyil. Gözlənilən: "
                            + expected.toPlainString() + " AZN.");
        }
        String appliedType = discountResult.getAppliedDiscountType();
        if (appliedType != null && !appliedType.isBlank()) {
            boolean needsCashierReview = java.util.Set.of(
                    "STUDENT", "FAMILY", "PUSH30", "QAZI", "SHAHEED"
            ).contains(appliedType);
            for (Reservation r : reservations) {
                r.setDiscountType(appliedType);
                if (needsCashierReview) {
                    r.setReviewStatus("PENDING_REVIEW");
                }
            }
            reservationRepository.saveAll(reservations);
        }
        return expected;
    }

    private void syncReservations(Payment payment, PaymentStatus status) {
        if (payment.getReservationIds() == null || payment.getReservationIds().isBlank()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (String part : payment.getReservationIds().split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // Natamam reference-i atırıq.
            }
        }
        if (status == PaymentStatus.SUCCESS) {
            reservationService.confirmReservations(ids);
        } else if (status == PaymentStatus.FAILED) {
            reservationService.cancelReservations(ids);
        }
    }

    private Payment getOwnedPayment(String transactionId, String username) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transactionId: " + transactionId));

        if (payment.getUser() == null || !payment.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Bu ödəniş sizə aid deyil.");
        }
        return payment;
    }

    private String normalizeFullName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
    }

    private void applyStatus(Payment payment, PaymentStatus status) {
        if (status == null) {
            throw new BadRequestException("Ödəniş statusu tələb olunur.");
        }
        payment.setStatus(status);
        if (status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILED) {
            payment.setCompletionDate(LocalDateTime.now());
        }
    }
}
