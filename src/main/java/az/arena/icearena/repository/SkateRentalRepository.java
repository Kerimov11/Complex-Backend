// File: src/main/java/az/arena/icearena/repository/SkateRentalRepository.java
package az.arena.icearena.repository;

import az.arena.icearena.entity.SkateRental;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkateRentalRepository extends JpaRepository<SkateRental, Long> {
    List<SkateRental> findByUserId(Long userId);

    List<SkateRental> findBySessionId(Long sessionId);
}
