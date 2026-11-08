package az.arena.icearena.dto.admin;

import az.arena.icearena.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffRequest {
    @NotBlank(message = "İstifadəçi adı tələb olunur")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "E-poçt tələb olunur")
    @Email(message = "E-poçt düzgün deyil")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Şifrə tələb olunur")
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "Ad tələb olunur")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur")
    @Size(max = 50)
    private String lastName;

    @Size(max = 20)
    private String phoneNumber;

    private Set<RoleName> roles;
}
