package az.arena.icearena.dto.admin;

import az.arena.icearena.entity.RoleName;
import jakarta.validation.constraints.Email;
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
public class UpdateStaffRequest {

    @NotBlank(message = "Ad tələb olunur")
    @Size(max = 50, message = "Ad maksimum 50 simvol ola bilər")
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur")
    @Size(max = 50, message = "Soyad maksimum 50 simvol ola bilər")
    private String lastName;

    @NotBlank(message = "İstifadəçi adı tələb olunur")
    @Size(min = 3, max = 50, message = "İstifadəçi adı 3-50 simvol olmalıdır")
    private String username;

    @NotBlank(message = "E-poçt tələb olunur")
    @Email(message = "Düzgün e-poçt ünvanı daxil edin")
    private String email;

    @Size(max = 30, message = "Telefon nömrəsi maksimum 30 simvol ola bilər")
    private String phoneNumber;

    private String password;

    private RoleName role;
}
