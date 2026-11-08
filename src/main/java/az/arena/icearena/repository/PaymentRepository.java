package az.arena.icearena.repository;

import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.PaymentChannel;
import az.arena.icearena.entity.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);

    List<Payment> findByChannelAndPaymentDateBetween(PaymentChannel channel,
                                                     LocalDateTime start,
                                                     LocalDateTime end);

    List<Payment> findByCashierUsernameAndPaymentDateBetween(String cashierUsername,
                                                             LocalDateTime start,
                                                             LocalDateTime end);
}
