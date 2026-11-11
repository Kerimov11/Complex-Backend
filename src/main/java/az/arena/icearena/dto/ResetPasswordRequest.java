package az.arena.icearena.dto;

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
public class ResetPasswordRequest {

    @NotBlank(message = "Bərpa üsulu seçilməlidir")
    private String method;

    @NotBlank(message = "E-poçt və ya telefon nömrəsi daxil edilməlidir")
    private String identifier;

    @NotBlank(message = "Təsdiq kodu daxil edilməlidir")
    private String code;

    @NotBlank(message = "Yeni şifrə daxil edilməlidir")
    @Size(min = 6, max = 100, message = "Yeni şifrə ən azı 6 simvoldan ibarət olmalıdır")
    private String newPassword;
}
