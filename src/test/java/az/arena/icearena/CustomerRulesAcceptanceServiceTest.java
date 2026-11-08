package az.arena.icearena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import az.arena.icearena.dto.customer.CustomerRulesAcceptanceDto;
import az.arena.icearena.entity.User;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CustomerRulesAcceptanceService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerRulesAcceptanceServiceTest {

    @Autowired
    private CustomerRulesAcceptanceService acceptanceService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void acceptanceIsStoredAndRestoredForCustomer() {
        User user = userRepository.save(User.builder()
                .username("rules-acceptance-test-user")
                .email("rules-acceptance-test-user@icearena.az")
                .password("x")
                .firstName("Rules")
                .lastName("Test")
                .build());

        CustomerRulesAcceptanceDto initial = acceptanceService.getForUser(user.getUsername());
        assertEquals(0, initial.getAcceptedCount());
        assertEquals(4, initial.getTotalCount());
        assertFalse(initial.isConfirmed());

        CustomerRulesAcceptanceDto partial = acceptanceService.saveForUser(
                user.getUsername(),
                Set.of("rules", "responsibility"));
        assertEquals(2, partial.getAcceptedCount());
        assertFalse(partial.isConfirmed());
        assertNotNull(partial.getUpdatedAt());

        CustomerRulesAcceptanceDto restored = acceptanceService.getForUser(user.getUsername());
        assertEquals(Set.of("rules", "responsibility"), restored.getAcceptedObligationIds());
        assertFalse(restored.isConfirmed());

        CustomerRulesAcceptanceDto complete = acceptanceService.saveForUser(
                user.getUsername(),
                Set.of("rules", "responsibility", "noClaim", "media"));
        assertEquals(4, complete.getAcceptedCount());
        assertTrue(complete.isConfirmed());

        CustomerRulesAcceptanceDto restoredComplete =
                acceptanceService.getForUser(user.getUsername());
        assertEquals(4, restoredComplete.getAcceptedCount());
        assertTrue(restoredComplete.isConfirmed());
    }
}
