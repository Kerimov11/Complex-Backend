package az.arena.icearena.dto.customer;

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
public class CustomerProfileDto {
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Integer age;
    private Boolean ageVerified;
    private Boolean isAdult;
    private Boolean identityConfirmed;
    private String identityDocumentLast4;
    private Set<String> roles;
}
