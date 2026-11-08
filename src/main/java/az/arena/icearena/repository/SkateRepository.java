package az.arena.icearena.repository;

import az.arena.icearena.entity.Skate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SkateRepository extends JpaRepository<Skate, Long> {
    Optional<Skate> findBySize(Integer size);
}