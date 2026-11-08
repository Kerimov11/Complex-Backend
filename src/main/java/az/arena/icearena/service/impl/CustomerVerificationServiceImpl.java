package az.arena.icearena.service.impl;

import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CustomerVerificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerVerificationServiceImpl implements CustomerVerificationService {

    public static final int ADULT_AGE = 18;
    private static final int MAX_AGE = 120;
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("^[A-Z0-9]{5,30}$");
    private static final Set<RoleName> STAFF_ROLES = Set.of(
            RoleName.ROLE_ADMIN,
            RoleName.ROLE_COORDINATOR,
            RoleName.ROLE_CASHIER,
            RoleName.ROLE_VOLUNTEER);

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerVerificationDto getForUser(String username) {
        return toDto(findUser(username));
    }

    @Override
    @Transactional
    public CustomerVerificationDto saveAge(String username, LocalDate dateOfBirth) {
        User user = findUser(username);

        if (user.getDateOfBirth() != null) {
            throw new BadRequestException(
                    "Yaş məlumatı artıq qeyd edilib və dəyişdirilə bilməz.");
        }
        if (dateOfBirth == null) {
            throw new BadRequestException("Doğum tarixi tələb olunur.");
        }

        LocalDate today = LocalDate.now();
        if (dateOfBirth.isAfter(today)) {
            throw new BadRequestException("Doğum tarixi gələcəkdə ola bilməz.");
        }

        int age = ageOf(dateOfBirth);
        if (age > MAX_AGE) {
            throw new BadRequestException("Doğum tarixi düzgün deyil.");
        }

        user.setDateOfBirth(dateOfBirth);
        user.setAgeVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        return toDto(user);
    }

    @Override
    @Transactional
    public CustomerVerificationDto saveIdentity(String username,
                                                String identityDocumentNumber,
                                                boolean confirmed) {
        User user = findUser(username);

        if (user.getDateOfBirth() == null) {
            throw new BadRequestException(
                    "Şəxsiyyət vəsiqəsini təsdiqləməzdən əvvəl yaşınızı təsdiqləyin.");
        }
        if (ageOf(user.getDateOfBirth()) < ADULT_AGE) {
            throw new BadRequestException(
                    "18 yaşdan kiçik şəxslər bilet ala bilməz və şəxsiyyət təsdiqi aparıla bilməz.");
        }
        if (!confirmed) {
            throw new BadRequestException("Şəxsiyyət vəsiqəsinin təsdiqi tələb olunur.");
        }

        String normalized = normalizeDocument(identityDocumentNumber);
        if (!DOCUMENT_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(
                    "Şəxsiyyət vəsiqəsinin seriya və nömrəsini düzgün qeyd edin.");
        }

        user.setIdentityDocumentNumber(normalized);
        user.setIdentityConfirmedAt(LocalDateTime.now());
        userRepository.save(user);
        return toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireTicketPurchaseEligibility(User user) {
        if (user == null || isStaff(user)) {
            return;
        }
        if (user.getDateOfBirth() == null) {
            throw new BadRequestException(
                    "Bilet almaq üçün əvvəlcə yaşınızı təsdiqləyin.");
        }

        int age = ageOf(user.getDateOfBirth());
        if (age < ADULT_AGE) {
            throw new BadRequestException(
                    "18 yaşdan kiçik şəxslər bilet ala bilməz.");
        }
        if (user.getIdentityConfirmedAt() == null
                || user.getIdentityDocumentNumber() == null
                || user.getIdentityDocumentNumber().isBlank()) {
            throw new BadRequestException(
                    "Bilet almaq üçün şəxsiyyət vəsiqəsini təsdiqləyin.");
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "İstifadəçi tapılmadı: " + username));
    }

    private int ageOf(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private boolean isStaff(User user) {
        if (user.getRoles() == null) {
            return false;
        }
        for (Role role : user.getRoles()) {
            if (role != null && STAFF_ROLES.contains(role.getName())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeDocument(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private CustomerVerificationDto toDto(User user) {
        LocalDate dateOfBirth = user.getDateOfBirth();
        boolean ageVerified = dateOfBirth != null;
        int age = ageVerified ? ageOf(dateOfBirth) : 0;
        boolean adult = ageVerified && age >= ADULT_AGE;
        boolean identityConfirmed = user.getIdentityConfirmedAt() != null
                && user.getIdentityDocumentNumber() != null
                && !user.getIdentityDocumentNumber().isBlank();

        String message;
        if (!ageVerified) {
            message = "Yaş təsdiqi gözləyir.";
        } else if (!adult) {
            message = "18 yaşdan kiçik şəxslər bilet ala bilməz.";
        } else if (!identityConfirmed) {
            message = "Bilet almaq üçün şəxsiyyət vəsiqəsini təsdiqləyin.";
        } else {
            message = "Yaş və şəxsiyyət vəsiqəsi təsdiqlənib.";
        }

        return CustomerVerificationDto.builder()
                .dateOfBirth(dateOfBirth)
                .age(ageVerified ? age : null)
                .ageVerified(ageVerified)
                .isAdult(adult)
                .identityConfirmed(identityConfirmed)
                .identityConfirmedAt(user.getIdentityConfirmedAt())
                .identityDocumentLast4(lastFour(user.getIdentityDocumentNumber()))
                .message(message)
                .build();
    }

    private String lastFour(String value) {
        if (value == null || value.length() < 4) {
            return "";
        }
        return value.substring(value.length() - 4);
    }
}
