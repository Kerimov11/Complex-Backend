// File: src/main/java/az/arena/icearena/dto/AuthenticationResponse.java
package az.arena.icearena.dto;

import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {
    private Long userId;
    private String token;
    private String username;
    private Set<String> roles;
    private String message;

    // Müştəri yaş / şəxsiyyət vəsiqəsi təsdiqi
    private LocalDate dateOfBirth;
    private Integer age;
    private Boolean ageVerified;
    private Boolean isAdult;
    private Boolean identityConfirmed;
    private String identityDocumentLast4;
}
