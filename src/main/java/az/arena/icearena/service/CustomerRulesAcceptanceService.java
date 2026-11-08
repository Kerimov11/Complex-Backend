package az.arena.icearena.service;

import az.arena.icearena.dto.customer.CustomerRulesAcceptanceDto;
import java.util.Set;

public interface CustomerRulesAcceptanceService {

    CustomerRulesAcceptanceDto getForUser(String username);

    CustomerRulesAcceptanceDto saveForUser(String username, Set<String> acceptedObligationIds);
}
