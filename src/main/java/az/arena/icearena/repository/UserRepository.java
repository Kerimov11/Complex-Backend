// File: src/main/java/az/arena/icearena/repository/UserRepository.java
package az.arena.icearena.repository;

import az.arena.icearena.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByIdentityReviewStatusOrderByIdentityConfirmedAtDesc(String identityReviewStatus);

    List<User> findByIdentityReviewStatusIsNotNullOrderByIdentityConfirmedAtDesc();
}
