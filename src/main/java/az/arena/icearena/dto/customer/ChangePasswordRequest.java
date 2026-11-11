package az.arena.icearena.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Cari şifrə tələb olunur.")
    private String currentPassword;

    @NotBlank(message = "Yeni şifrə tələb olunur.")
    @Size(min = 6, max = 100, message = "Yeni şifrə ən azı 6 simvoldan ibarət olmalıdır.")
    private String newPassword;

    @NotBlank(message = "Yeni şifrənin təkrarı tələb olunur.")
    private String confirmPassword;
}
