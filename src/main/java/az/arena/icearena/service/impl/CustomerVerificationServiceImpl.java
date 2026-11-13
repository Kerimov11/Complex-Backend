package az.arena.icearena.service.impl;

import az.arena.icearena.dto.customer.CustomerVerificationDto;
import az.arena.icearena.dto.customer.TicketPhotosRequest;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CustomerVerificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
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
    private static final Pattern FIN_PATTERN = Pattern.compile("^[A-Z0-9]{7}$");
    private static final int MAX_PHOTO_DATA_LENGTH = 6_000_000;
    private static final Set<RoleName> STAFF_ROLES = Set.of(
            RoleName.ROLE_ADMIN,
            RoleName.ROLE_COORDINATOR,
            RoleName.ROLE_CASHIER,
            RoleName.ROLE_VOLUNTEER);

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

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
                                                String finCode,
                                                String identityPhotoData,
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

        String normalizedFin = normalizeDocument(finCode);
        if (!FIN_PATTERN.matcher(normalizedFin).matches()) {
            throw new BadRequestException(
                    "FİN kodu 7 simvoldan ibarət olmalıdır (məs: 5ABC123).");
        }

        String cleanedPhoto = identityPhotoData == null ? "" : identityPhotoData.trim();
        if (!cleanedPhoto.startsWith("data:image/") || cleanedPhoto.length() < 100) {
            throw new BadRequestException(
                    "Şəxsiyyət vəsiqəsi ilə foto təsdiqi tələb olunur.");
        }
        if (cleanedPhoto.length() > MAX_PHOTO_DATA_LENGTH) {
            throw new BadRequestException(
                    "Yüklənən şəkil həddindən artıq böyükdür.");
        }

        user.setIdentityDocumentNumber(normalized);
        user.setFinCode(normalizedFin);
        user.setIdentityPhotoData(cleanedPhoto);
        user.setIdentityConfirmedAt(LocalDateTime.now());
        user.setIdentityReviewStatus("PENDING_REVIEW");
        user.setIdentityReviewedAt(null);
        user.setIdentityReviewedBy(null);
        user.setIdentityRejectionReason(null);
        userRepository.save(user);
        return toDto(user);
    }

    @Override
    @Transactional
    public CustomerVerificationDto saveTicketPhotos(String username, TicketPhotosRequest request) {
        User user = findUser(username);
        if (request == null || request.getVoucherCode() == null || request.getVoucherCode().isBlank()) {
            throw new BadRequestException("Bilet kodu tələb olunur.");
        }
        List<Reservation> reservations = reservationRepository
                .findByVoucherCodeIgnoreCase(request.getVoucherCode().trim());
        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("Bilet tapılmadı.");
        }

        List<TicketPhotosRequest.ParticipantPhoto> items =
                request.getParticipants() == null ? List.of() : request.getParticipants();
        boolean anyPhotoSubmitted = false;

        String familyPhoto = request.getFamilyIdsPhotoData() == null
                ? "" : request.getFamilyIdsPhotoData().trim();
        String push30Photo = request.getPush30ScreenshotData() == null
                ? "" : request.getPush30ScreenshotData().trim();
        String push30Phone = request.getPush30Phone() == null
                ? "" : request.getPush30Phone().replaceAll("[^0-9]", "");
        String qaziPhoto = request.getQaziDocumentPhotoData() == null
                ? "" : request.getQaziDocumentPhotoData().trim();
        String qaziFullName = request.getQaziFullName() == null
                ? "" : request.getQaziFullName().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        String shaheedPhoto = request.getShaheedDocumentPhotoData() == null
                ? "" : request.getShaheedDocumentPhotoData().trim();

        if (!push30Phone.isEmpty()) {
            boolean usedElsewhere = reservationRepository.existsByPush30PhoneAndStatusNot(
                    push30Phone, Reservation.Status.CANCELLED)
                    && reservations.stream().noneMatch(r -> push30Phone.equals(r.getPush30Phone()));
            if (usedElsewhere) {
                throw new BadRequestException(
                        "Bu nömrə ilə PUSH30 endirimindən artıq istifadə olunub. PUSH30 endirimi yalnız 1 gedişlikdir.");
            }
        }

        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            if (reservation.getUser() == null
                    || !reservation.getUser().getId().equals(user.getId())) {
                continue;
            }
            if (familyPhoto.startsWith("data:image/") && familyPhoto.length() <= MAX_PHOTO_DATA_LENGTH) {
                reservation.setFamilyIdsPhotoData(familyPhoto);
                anyPhotoSubmitted = true;
            }
            if (push30Photo.startsWith("data:image/") && push30Photo.length() <= MAX_PHOTO_DATA_LENGTH) {
                reservation.setPush30ScreenshotData(push30Photo);
                anyPhotoSubmitted = true;
            }
            if (!push30Phone.isEmpty()) {
                reservation.setPush30Phone(push30Phone);
            }
            if (qaziPhoto.startsWith("data:image/") && qaziPhoto.length() <= MAX_PHOTO_DATA_LENGTH) {
                reservation.setQaziDocumentPhotoData(qaziPhoto);
                anyPhotoSubmitted = true;
            }
            if (!qaziFullName.isEmpty()) {
                reservation.setQaziFullName(qaziFullName);
            }
            if (shaheedPhoto.startsWith("data:image/") && shaheedPhoto.length() <= MAX_PHOTO_DATA_LENGTH) {
                reservation.setShaheedDocumentPhotoData(shaheedPhoto);
                anyPhotoSubmitted = true;
            }
            TicketPhotosRequest.ParticipantPhoto match = items.stream()
                    .filter(p -> p.getReservationId() != null
                            && p.getReservationId().equals(reservation.getId()))
                    .findFirst()
                    .orElse(i < items.size() ? items.get(i) : null);
            if (match == null) {
                continue;
            }
            if (match.getIdentityPhotoData() != null
                    && match.getIdentityPhotoData().trim().startsWith("data:image/")) {
                String idPhoto = match.getIdentityPhotoData().trim();
                if (idPhoto.length() <= MAX_PHOTO_DATA_LENGTH) {
                    reservation.setIdentityPhotoData(idPhoto);
                    anyPhotoSubmitted = true;
                }
            }
            if (match.getStudentCardPhotoData() != null
                    && match.getStudentCardPhotoData().trim().startsWith("data:image/")) {
                String stPhoto = match.getStudentCardPhotoData().trim();
                if (stPhoto.length() <= MAX_PHOTO_DATA_LENGTH) {
                    reservation.setStudentCardPhotoData(stPhoto);
                    reservation.setStudentDiscountApplied(true);
                    anyPhotoSubmitted = true;
                }
            } else if (Boolean.TRUE.equals(match.getStudentDiscountApplied())) {
                reservation.setStudentDiscountApplied(true);
            }
            reservationRepository.save(reservation);
        }

        if (anyPhotoSubmitted) {
            user.setIdentityReviewStatus("PENDING_REVIEW");
            user.setIdentityReviewedAt(null);
            user.setIdentityReviewedBy(null);
            user.setIdentityRejectionReason(null);
            userRepository.save(user);
        }
        return toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPush30PhoneUsed(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (normalized.length() < 9) {
            return false;
        }
        return reservationRepository.existsByPush30PhoneAndStatusNot(
                normalized, Reservation.Status.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getQaziVisitCount(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return 0L;
        }
        String normalized = fullName.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        return reservationRepository.countDistinctQaziVisits(
                normalized, Reservation.Status.CANCELLED);
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
        if ("REJECTED".equals(user.getIdentityReviewStatus())) {
            throw new BadRequestException(
                    "Şəxsiyyət və foto təsdiqiniz rədd edilib. Zəhmət olmasa yeni foto yükləyin.");
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
                && !user.getIdentityDocumentNumber().isBlank()
                && !"REJECTED".equals(user.getIdentityReviewStatus());
        String reviewStatus = user.getIdentityReviewStatus();

        String message;
        if (!ageVerified) {
            message = "Yaş təsdiqi gözləyir.";
        } else if (!adult) {
            message = "18 yaşdan kiçik şəxslər bilet ala bilməz.";
        } else if ("REJECTED".equals(reviewStatus)) {
            message = "Şəxsiyyət təsdiqi rədd edilib. Zəhmət olmasa yeni foto yükləyin.";
        } else if (!identityConfirmed) {
            message = "Bilet almaq üçün şəxsiyyət vəsiqəsini təsdiqləyin.";
        } else if ("PENDING_REVIEW".equals(reviewStatus)) {
            message = "Şəxsiyyət və foto məlumatlarınız kassir təsdiqini gözləyir.";
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
                .finCode(user.getFinCode())
                .identityReviewStatus(reviewStatus)
                .identityRejectionReason(user.getIdentityRejectionReason())
                .hasIdentityPhoto(user.getIdentityPhotoData() != null && !user.getIdentityPhotoData().isBlank())
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
