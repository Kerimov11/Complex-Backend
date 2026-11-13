package az.arena.icearena.controller.client;

import az.arena.icearena.dto.customer.ChangePasswordRequest;
import az.arena.icearena.dto.customer.CustomerProfileDto;
import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.dto.customer.UpdateCustomerProfileRequest;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CustomerVerificationService;
import az.arena.icearena.service.EmailValidationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerProfileController {

    private final UserRepository userRepository;
    private final CustomerVerificationService customerVerificationService;
    private final EmailValidationService emailValidationService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<CustomerProfileDto> getProfile(Principal principal) {
        User user = findUser(principal.getName());
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<CustomerProfileDto> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            Principal principal) {
        User user = findUser(principal.getName());

        String nextEmail = request.getEmail().trim();
        if (!nextEmail.equalsIgnoreCase(user.getEmail())) {
            emailValidationService.validate(nextEmail);
            if (userRepository.existsByEmail(nextEmail)) {
                throw new BadRequestException("Bu e-poçt ünvanı artıq başqa hesabda istifadə olunur.");
            }
        }

        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new BadRequestException("Doğum tarixi gələcək tarix ola bilməz.");
            }
            user.setDateOfBirth(request.getDateOfBirth());
            if (user.getAgeVerifiedAt() == null) {
                user.setAgeVerifiedAt(LocalDateTime.now());
            }
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(nextEmail);
        user.setPhoneNumber(
                request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim());

        userRepository.save(user);
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping("/password")
    @Transactional
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        User user = findUser(principal.getName());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Cari şifrə yanlışdır.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Yeni şifrə və təkrarı eyni deyil.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Yeni şifrə cari şifrədən fərqli olmalıdır.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Şifrəniz uğurla yeniləndi."));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + username));
    }

    private CustomerProfileDto toDto(User user) {
        CustomerVerificationDto verification =
                customerVerificationService.getForUser(user.getUsername());
        return CustomerProfileDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(verification.getDateOfBirth())
                .age(verification.getAge())
                .ageVerified(verification.getAgeVerified())
                .isAdult(verification.getIsAdult())
                .identityConfirmed(verification.getIdentityConfirmed())
                .identityDocumentLast4(verification.getIdentityDocumentLast4())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .build();
    }
}
