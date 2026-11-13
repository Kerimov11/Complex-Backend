package az.arena.icearena.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityVerificationRequest {

    @NotBlank(message = "Şəxsiyyət vəsiqəsinin seriya və nömrəsi tələb olunur.")
    @Size(min = 5, max = 30, message = "Şəxsiyyət vəsiqəsinin nömrəsi 5-30 simvol olmalıdır.")
    private String identityDocumentNumber;

    @NotBlank(message = "FİN kod tələb olunur.")
    @Size(min = 7, max = 16, message = "FİN kod 7 simvol olmalıdır.")
    private String finCode;

    @NotBlank(message = "Şəxsiyyət vəsiqəsi ilə foto təsdiqi tələb olunur.")
    private String identityPhotoData;

    @NotNull(message = "Şəxsiyyət vəsiqəsi təsdiqi tələb olunur.")
    private Boolean confirmed;
}
