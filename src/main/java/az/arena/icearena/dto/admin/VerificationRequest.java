package az.arena.icearena.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequest {
    @NotBlank(message = "QR / bilet kodu tələb olunur")
    private String code;
}
