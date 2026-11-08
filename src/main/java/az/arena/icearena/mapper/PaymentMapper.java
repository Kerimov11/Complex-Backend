package az.arena.icearena.mapper;

import az.arena.icearena.dto.PaymentDto;
import az.arena.icearena.entity.Payment;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDto toDto(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentDto.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .reference(payment.getReference())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .completionDate(payment.getCompletionDate())
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .username(payment.getUser() != null ? payment.getUser().getUsername() : null)
                .ticketId(payment.getTicket() != null ? payment.getTicket().getId() : null)
                .skateRentalId(payment.getSkateRental() != null ? payment.getSkateRental().getId() : null)
                .reservationIds(parseIds(payment.getReservationIds()))
                .channel(payment.getChannel())
                .paymentMethod(payment.getPaymentMethod())
                .cashierUsername(payment.getCashierUsername())
                .customerName(payment.getCustomerName())
                .quantity(payment.getQuantity())
                .build();
    }

    public List<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // Natamam/korlanmış reference-i sadəcə atırıq.
            }
        }
        return ids;
    }
}
