package az.arena.icearena.repository;

import az.arena.icearena.entity.Ticket;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByQrCode(String qrCode);

    Optional<Ticket> findByTicketCodeIgnoreCase(String ticketCode);

    Optional<Ticket> findByQrCodeIgnoreCase(String qrCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where upper(t.ticketCode) = upper(:code)")
    Optional<Ticket> findByTicketCodeIgnoreCaseForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where upper(t.qrCode) = upper(:code)")
    Optional<Ticket> findByQrCodeIgnoreCaseForUpdate(@Param("code") String code);

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findBySessionId(Long sessionId);

    List<Ticket> findByPurchaseDateBetween(LocalDateTime start, LocalDateTime end);

    long countByIsUsedTrue();
}
