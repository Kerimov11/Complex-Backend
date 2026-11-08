package az.arena.icearena.repository;

import az.arena.icearena.entity.CustomerRulesAcceptance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRulesAcceptanceRepository
        extends JpaRepository<CustomerRulesAcceptance, Long> {

    Optional<CustomerRulesAcceptance> findByUserUsername(String username);
}
