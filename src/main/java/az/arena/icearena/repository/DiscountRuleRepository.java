package az.arena.icearena.repository;

import az.arena.icearena.entity.DiscountRule;
import az.arena.icearena.entity.DiscountType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    List<DiscountRule> findByActiveTrue();

    Optional<DiscountRule> findByType(DiscountType type);
}
