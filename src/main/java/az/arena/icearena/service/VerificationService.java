package az.arena.icearena.service;

import az.arena.icearena.dto.admin.ActiveGuestsDto;
import az.arena.icearena.dto.admin.VerificationResultDto;

public interface VerificationService {
    VerificationResultDto verify(String code, String username);

    VerificationResultDto lookup(String code);

    default VerificationResultDto lookup(String code, String username) {
        return lookup(code);
    }

    VerificationResultDto handover(String code, String username);

    ActiveGuestsDto activeGuests();
}
