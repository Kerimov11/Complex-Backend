package az.arena.icearena.service;

import az.arena.icearena.dto.BookingPaymentRequest;
import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {
    Payment createPaymentForTicket(Long ticketId, BigDecimal amount);

    Payment createPaymentForSkateRental(Long skateRentalId, BigDecimal amount);

    /** Köhnə çağırışlar üçün uyğunluq. */
    Payment createBookingPayment(String username, BigDecimal amount, String reference);

    Payment createBookingPayment(String username, BookingPaymentRequest request);

    Payment processPayment(String transactionId, PaymentStatus status);

    Payment processOwnPayment(String transactionId, String username, PaymentStatus status);

    Payment getPaymentByTransactionId(String transactionId);

    Payment getPaymentForUser(String transactionId, String username);

    List<Payment> getPayments(PaymentStatus status);
}
