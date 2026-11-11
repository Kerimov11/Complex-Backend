// File: src/main/java/az/arena/icearena/service/AuthService.java
package az.arena.icearena.service;

import az.arena.icearena.dto.AuthenticationRequest;
import az.arena.icearena.dto.AuthenticationResponse;
import az.arena.icearena.dto.ForgotPasswordRequest;
import az.arena.icearena.dto.ForgotPasswordResponse;
import az.arena.icearena.dto.RegisterRequest;
import az.arena.icearena.dto.ResetPasswordRequest;

public interface AuthService {
    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request);

    AuthenticationResponse resetPassword(ResetPasswordRequest request);
}
