package az.arena.icearena.dto.customer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeVerificationRequest {

    @NotNull(message = "Doğum tarixi tələb olunur.")
    @PastOrPresent(message = "Doğum tarixi gələcəkdə ola bilməz.")
    private LocalDate dateOfBirth;
}
