// File: src/main/java/az/arena/icearena/repository/SessionRepository.java
package az.arena.icearena.repository;

import az.arena.icearena.entity.Session;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findBySessionDate(LocalDate sessionDate);

    List<Session> findBySessionDateBetween(LocalDate startDate, LocalDate endDate);
}
