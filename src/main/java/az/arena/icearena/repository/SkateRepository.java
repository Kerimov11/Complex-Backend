package az.arena.icearena.repository;

import az.arena.icearena.entity.Skate;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkateRepository extends JpaRepository<Skate, Long> {
    Optional<Skate> findBySize(Integer size);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Skate s where s.id = :id")
    Optional<Skate> findByIdForUpdate(@Param("id") Long id);
}