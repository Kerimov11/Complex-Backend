// File: src/main/java/az/arena/icearena/service/impl/AuthServiceImpl.java
package az.arena.icearena.service.impl;

import az.arena.icearena.config.JwtService;
import az.arena.icearena.dto.AuthenticationRequest;
import az.arena.icearena.dto.AuthenticationResponse;
import az.arena.icearena.dto.ForgotPasswordRequest;
import az.arena.icearena.dto.ForgotPasswordResponse;
import az.arena.icearena.dto.RegisterRequest;
import az.arena.icearena.dto.ResetPasswordRequest;
import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.repository.RoleRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.AuthService;
import az.arena.icearena.service.CustomerVerificationService;
import az.arena.icearena.service.EmailValidationService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomerVerificationService customerVerificationService;
    private final EmailValidationService emailValidationService;

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        emailValidationService.validate(request.getEmail());
        String normalizedEmail = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Bu istifadəçi adı artıq mövcuddur.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)
                || isCanonicalGmailUsed(normalizedEmail)) {
            throw new BadRequestException("Bu e-poçt ünvanı ilə artıq hesab mövcuddur.");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("Telefon nömrəsi tələb olunur.");
        }
        String normalizedPhoneDigits = normalizePhoneDigits(request.getPhoneNumber());
        if (normalizedPhoneDigits.length() < 9) {
            throw new BadRequestException("Telefon nömrəsini düzgün daxil edin.");
        }
        boolean phoneUsed = userRepository.existsByPhoneNumber(request.getPhoneNumber().trim())
                || userRepository.findByPhoneNumberIsNotNull().stream()
                .map(User::getPhoneNumber)
                .map(this::normalizePhoneDigits)
                .anyMatch(existing -> !existing.isEmpty() && existing.equals(normalizedPhoneDigits));
        if (phoneUsed) {
            throw new BadRequestException("Bu telefon nömrəsi ilə artıq hesab mövcuddur.");
        }
        String storedPhone = normalizedPhoneDigits.length() == 12 && normalizedPhoneDigits.startsWith("994")
                ? "+" + normalizedPhoneDigits
                : request.getPhoneNumber().trim();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BadRequestException("Default role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        java.time.LocalDate dob = request.getDateOfBirth();
        if (dob != null) {
            if (dob.isAfter(java.time.LocalDate.now())) {
                throw new BadRequestException("Doğum tarixi gələcəkdə ola bilməz.");
            }
            int years = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
            if (years > 120) {
                throw new BadRequestException("Doğum tarixi düzgün deyil.");
            }
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(storedPhone)
                .dateOfBirth(dob)
                .ageVerifiedAt(dob != null ? java.time.LocalDateTime.now() : null)
                .roles(roles)
                .build();

        userRepository.saveAndFlush(user);

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

    @Override
    @Transactional
    public ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request) {
        String method = normalizeMethod(request.getMethod());
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        if (identifier.isEmpty()) {
            throw new BadRequestException(
                    "EMAIL".equals(method)
                            ? "Zəhmət olmasa Gmail (e-poçt) ünvanınızı daxil edin."
                            : "Zəhmət olmasa telefon nömrənizi daxil edin.");
        }

        User user = findUserForReset(method, identifier);
        String code = String.format("%06d", 100000 + RANDOM.nextInt(900000));
        user.setPasswordResetCode(code);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.saveAndFlush(user);

        String masked = "EMAIL".equals(method)
                ? maskEmail(user.getEmail())
                : maskPhone(user.getPhoneNumber());

        log.info("Şifrə bərpa kodu yaradıldı: user={}, method={}, target={}, code={}",
                user.getUsername(), method, masked, code);

        String message = "EMAIL".equals(method)
                ? "Şifrəni yeniləmək üçün təsdiq kodu Gmail ünvanınıza (" + masked + ") göndərildi."
                : "Şifrəni yeniləmək üçün təsdiq kodu telefon nömrənizə (" + masked + ") SMS olaraq göndərildi.";

        return ForgotPasswordResponse.builder()
                .message(message)
                .method(method)
                .maskedTarget(masked)
                .username(user.getUsername())
                .demoResetCode(code)
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse resetPassword(ResetPasswordRequest request) {
        String method = normalizeMethod(request.getMethod());
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        String code = request.getCode() == null ? "" : request.getCode().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword();

        if (newPassword.length() < 6) {
            throw new BadRequestException("Yeni şifrə ən azı 6 simvoldan ibarət olmalıdır.");
        }

        User user = findUserForReset(method, identifier);
        if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(code)) {
            throw new BadRequestException("Daxil edilən təsdiq kodu yanlışdır.");
        }
        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Təsdiq kodunun vaxtı bitib. Zəhmət olmasa yenidən kod göndərin.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.saveAndFlush(user);

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
        return response(user, jwtToken, "Şifrəniz uğurla yeniləndi.");
    }

    private String normalizeMethod(String method) {
        if (method != null && "PHONE".equalsIgnoreCase(method.trim())) {
            return "PHONE";
        }
        return "EMAIL";
    }

    private User findUserForReset(String method, String identifier) {
        if ("PHONE".equals(method)) {
            String targetDigits = digitsOnly(identifier);
            if (targetDigits.length() < 7) {
                throw new BadRequestException("Telefon nömrəsini düzgün daxil edin.");
            }
            String targetTail = targetDigits.length() > 9
                    ? targetDigits.substring(targetDigits.length() - 9)
                    : targetDigits;
            return userRepository.findAll().stream()
                    .filter(u -> {
                        String uDigits = digitsOnly(u.getPhoneNumber());
                        if (uDigits.isEmpty()) return false;
                        String uTail = uDigits.length() > 9
                                ? uDigits.substring(uDigits.length() - 9)
                                : uDigits;
                        return uDigits.equals(targetDigits) || uTail.equals(targetTail);
                    })
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "Bu telefon nömrəsinə uyğun hesab tapılmadı."));
        }

        String needle = identifier.toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null
                        && u.getEmail().trim().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Bu Gmail (e-poçt) ünvanına uyğun hesab tapılmadı."));
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        return value.replaceAll("[^0-9]", "");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        String trimmed = phone.trim();
        if (trimmed.length() <= 6) return "***" + trimmed.substring(trimmed.length() - 2);
        return trimmed.substring(0, 4) + " *** ** " + trimmed.substring(trimmed.length() - 2);
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

    private boolean isCanonicalGmailUsed(String normalizedEmail) {
        String target = canonicalizeGmail(normalizedEmail);
        if (target == null) {
            return false;
        }
        return userRepository.findAll().stream()
                .map(User::getEmail)
                .map(this::canonicalizeGmail)
                .anyMatch(target::equals);
    }

    private String canonicalizeGmail(String email) {
        if (email == null) {
            return null;
        }
        String lower = email.trim().toLowerCase(java.util.Locale.ROOT);
        int at = lower.indexOf('@');
        if (at <= 0) {
            return null;
        }
        String local = lower.substring(0, at);
        String domain = lower.substring(at + 1);
        if (!"gmail.com".equals(domain) && !"googlemail.com".equals(domain)) {
            return null;
        }
        int plus = local.indexOf('+');
        if (plus >= 0) {
            local = local.substring(0, plus);
        }
        local = local.replace(".", "");
        return local + "@gmail.com";
    }

    private String normalizePhoneDigits(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 10 && digits.startsWith("0")) {
            return "994" + digits.substring(1);
        }
        if (digits.length() == 9) {
            return "994" + digits;
        }
        return digits;
    }
}
