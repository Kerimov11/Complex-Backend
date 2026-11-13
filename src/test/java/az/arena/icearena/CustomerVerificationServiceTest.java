package az.arena.icearena;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CustomerVerificationService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerVerificationServiceTest {

    @Autowired
    private CustomerVerificationService verificationService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void underageCustomerCannotBuyTickets() {
        User user = createUser("age-minor-test");

        CustomerVerificationDto result = verificationService.saveAge(
                user.getUsername(),
                LocalDate.now().minusYears(17));

        assertTrue(result.getAgeVerified());
        assertFalse(result.getIsAdult());
        assertEquals(17, result.getAge());
        assertThrows(BadRequestException.class,
                () -> verificationService.requireTicketPurchaseEligibility(user));
    }

    @Test
    void adultMustConfirmIdentityBeforeBuyingTickets() {
        User user = createUser("age-adult-test");

        CustomerVerificationDto ageResult = verificationService.saveAge(
                user.getUsername(),
                LocalDate.now().minusYears(30));
        assertTrue(ageResult.getAgeVerified());
        assertTrue(ageResult.getIsAdult());
        assertFalse(ageResult.getIdentityConfirmed());

        assertThrows(BadRequestException.class,
                () -> verificationService.requireTicketPurchaseEligibility(user));

        CustomerVerificationDto identityResult = verificationService.saveIdentity(
                user.getUsername(),
                "AA 1234567",
                "5ABC123",
                "data:image/jpeg;base64," + "A".repeat(120),
                true);
        assertTrue(identityResult.getIdentityConfirmed());
        assertEquals("4567", identityResult.getIdentityDocumentLast4());

        assertDoesNotThrow(
                () -> verificationService.requireTicketPurchaseEligibility(user));
    }

    @Test
    void underageCustomerCannotConfirmIdentity() {
        User user = createUser("age-minor-identity-test");
        verificationService.saveAge(user.getUsername(), LocalDate.now().minusYears(10));

        assertThrows(BadRequestException.class,
                () -> verificationService.saveIdentity(
                        user.getUsername(),
                        "AA1234567",
                        "5ABC123",
                        "data:image/jpeg;base64," + "A".repeat(120),
                        true));
    }

    @Test
    void ageCanOnlyBeSetOnce() {
        User user = createUser("age-once-test");
        verificationService.saveAge(user.getUsername(), LocalDate.now().minusYears(20));

        assertThrows(BadRequestException.class,
                () -> verificationService.saveAge(
                        user.getUsername(),
                        LocalDate.now().minusYears(30)));
    }

    private User createUser(String username) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@icearena.az")
                .password("x")
                .firstName("Age")
                .lastName("Test")
                .build());
    }
}
