package az.arena.icearena.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    /** "EMAIL" və ya "PHONE" */
    @NotBlank(message = "Bərpa üsulu seçilməlidir")
    private String method;

    /** Gmail ünvanı və ya telefon nömrəsi */
    @NotBlank(message = "E-poçt və ya telefon nömrəsi daxil edilməlidir")
    private String identifier;
}
