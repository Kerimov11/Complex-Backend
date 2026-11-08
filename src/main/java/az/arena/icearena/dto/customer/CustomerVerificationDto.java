package az.arena.icearena.dto.customer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Müştərinin yaş və şəxsiyyət vəsiqəsi təsdiqi vəziyyəti.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVerificationDto {

    private LocalDate dateOfBirth;
    private Integer age;
    private Boolean ageVerified;
    private Boolean isAdult;
    private Boolean identityConfirmed;
    private LocalDateTime identityConfirmedAt;
    private String identityDocumentLast4;
    private String message;
}
