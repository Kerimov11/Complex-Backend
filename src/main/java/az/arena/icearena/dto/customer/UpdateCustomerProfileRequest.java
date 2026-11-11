package az.arena.icearena.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerProfileRequest {

    @NotBlank(message = "Ad tələb olunur.")
    @Size(max = 50, message = "Ad maksimum 50 simvol ola bilər.")
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur.")
    @Size(max = 50, message = "Soyad maksimum 50 simvol ola bilər.")
    private String lastName;

    @NotBlank(message = "E-poçt ünvanı tələb olunur.")
    @Email(message = "Düzgün e-poçt (Gmail) ünvanı daxil edin.")
    @Size(max = 100, message = "E-poçt maksimum 100 simvol ola bilər.")
    private String email;

    @Size(max = 20, message = "Telefon nömrəsi maksimum 20 simvol ola bilər.")
    private String phoneNumber;

    private LocalDate dateOfBirth;
}
