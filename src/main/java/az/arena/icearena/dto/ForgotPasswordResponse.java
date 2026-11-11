package az.arena.icearena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordResponse {
    private String message;
    private String method;
    private String maskedTarget;
    private String username;
    private String demoResetCode;
}
