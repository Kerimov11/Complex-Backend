package az.arena.icearena.dto.admin;

import az.arena.icearena.entity.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRoleUpdateRequest {

    @NotNull(message = "Yeni rol tələb olunur")
    private RoleName role;
}
