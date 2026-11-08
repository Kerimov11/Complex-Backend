package az.arena.icearena.dto.admin;

import az.arena.icearena.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleUpdateRequest {
    @NotEmpty(message = "Ən azı bir rol seçilməlidir")
    private Set<RoleName> roles;
}
