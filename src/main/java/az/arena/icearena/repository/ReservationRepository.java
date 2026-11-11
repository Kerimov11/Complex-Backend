package az.arena.icearena.repository;

import az.arena.icearena.entity.Reservation;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByVoucherCodeIgnoreCase(String voucherCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where upper(r.voucherCode) = upper(:voucherCode) order by r.id")
    List<Reservation> findByVoucherCodeIgnoreCaseForUpdate(
            @Param("voucherCode") String voucherCode);

    List<Reservation> findByCheckedInAtIsNotNull();

    List<Reservation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsBySkateId(Long skateId);

    long countBySessionIdAndStatusIn(Long sessionId, Collection<Reservation.Status> statuses);

    long countBySessionIdAndSkateIdAndStatusIn(Long sessionId, Long skateId,
                                               Collection<Reservation.Status> statuses);

    Optional<Reservation> findFirstByVoucherCodeIgnoreCase(String voucherCode);

    List<Reservation> findByStatusInAndSessionSessionDateGreaterThanEqualOrderBySessionSessionDateAscSessionStartTimeAscIdAsc(
            Collection<Reservation.Status> statuses, LocalDate date);

    List<Reservation> findBySkateStockDeductedTrueAndSkateHandedOutAtIsNullAndTimerExpiresAtBefore(
            LocalDateTime now);

    List<Reservation> findBySkateStockDeductedTrueAndSkateHandedOutAtIsNullAndTimerExpiresAtAfterOrderByTimerExpiresAtAsc(
            LocalDateTime now);
}
