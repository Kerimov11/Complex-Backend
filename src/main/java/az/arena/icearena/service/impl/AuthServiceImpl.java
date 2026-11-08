// File: src/main/java/az/arena/icearena/service/impl/AuthServiceImpl.java
package az.arena.icearena.service.impl;

import az.arena.icearena.config.JwtService;
import az.arena.icearena.dto.AuthenticationRequest;
import az.arena.icearena.dto.AuthenticationResponse;
import az.arena.icearena.dto.RegisterRequest;
import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.repository.RoleRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.AuthService;
import az.arena.icearena.service.CustomerVerificationService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomerVerificationService customerVerificationService;

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BadRequestException("Default role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .roles(roles)
                .build();

        userRepository.save(user);

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                .toList()
                );

        String jwtToken = jwtService.generateToken(userDetails);
        return response(user, jwtToken, "User registered successfully");
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                .toList()
                );

        String jwtToken = jwtService.generateToken(userDetails);
        return response(user, jwtToken, "Authentication successful");
    }

    private AuthenticationResponse response(User user, String jwtToken, String message) {
        CustomerVerificationDto verification =
                customerVerificationService.getForUser(user.getUsername());

        return AuthenticationResponse.builder()
                .userId(user.getId())
                .token(jwtToken)
                .username(user.getUsername())
                .roles(roleNames(user))
                .message(message)
                .dateOfBirth(verification.getDateOfBirth())
                .age(verification.getAge())
                .ageVerified(verification.getAgeVerified())
                .isAdult(verification.getIsAdult())
                .identityConfirmed(verification.getIdentityConfirmed())
                .identityDocumentLast4(verification.getIdentityDocumentLast4())
                .build();
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(java.util.stream.Collectors.toSet());
    }
}
