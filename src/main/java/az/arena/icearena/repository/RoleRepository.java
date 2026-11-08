// File: src/main/java/az/arena/icearena/repository/RoleRepository.java
package az.arena.icearena.repository;

import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
