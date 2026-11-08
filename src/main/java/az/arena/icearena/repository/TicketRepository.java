package az.arena.icearena.repository;

import az.arena.icearena.entity.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByQrCode(String qrCode);

    Optional<Ticket> findByTicketCodeIgnoreCase(String ticketCode);

    Optional<Ticket> findByQrCodeIgnoreCase(String qrCode);

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findBySessionId(Long sessionId);

    List<Ticket> findByPurchaseDateBetween(LocalDateTime start, LocalDateTime end);

    long countByIsUsedTrue();
}
