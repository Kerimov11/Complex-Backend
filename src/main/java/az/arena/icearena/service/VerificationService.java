package az.arena.icearena.service;

import az.arena.icearena.dto.admin.VerificationResultDto;

public interface VerificationService {
    VerificationResultDto verify(String code, String username);

    VerificationResultDto lookup(String code);
}
