package az.arena.icearena.dto.customer;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Data;

@Data
public class CustomerRulesAcceptanceRequest {

    @NotNull(message = "Təsdiqlənən öhdəliklər göndərilməlidir.")
    private Set<String> acceptedObligationIds;
}
