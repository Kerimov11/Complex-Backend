// File: src/main/java/az/arena/icearena/service/AuthService.java
package az.arena.icearena.service;

import az.arena.icearena.dto.AuthenticationRequest;
import az.arena.icearena.dto.AuthenticationResponse;
import az.arena.icearena.dto.RegisterRequest;

public interface AuthService {
    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);
}
