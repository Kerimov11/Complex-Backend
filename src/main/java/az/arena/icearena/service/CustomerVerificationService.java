package az.arena.icearena.service;

import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.entity.User;
import java.time.LocalDate;

public interface CustomerVerificationService {

    CustomerVerificationDto getForUser(String username);

    CustomerVerificationDto saveAge(String username, LocalDate dateOfBirth);

    CustomerVerificationDto saveIdentity(String username,
                                         String identityDocumentNumber,
                                         boolean confirmed);

    /**
     * Bilet almaq üçün müştərinin 18+ və şəxsiyyət vəsiqəsi təsdiqli
     * olmasını yoxlayır. Staff rolları bu yoxlamadan azaddır.
     */
    void requireTicketPurchaseEligibility(User user);
}
