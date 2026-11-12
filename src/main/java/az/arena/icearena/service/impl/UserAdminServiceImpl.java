package az.arena.icearena.service.impl;

import az.arena.icearena.dto.admin.CreateStaffRequest;
import az.arena.icearena.dto.admin.UpdateStaffRequest;
import az.arena.icearena.dto.admin.UserAdminDto;
import az.arena.icearena.dto.admin.UserRoleUpdateRequest;
import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.RoleRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.repository.VolunteerShiftRepository;
import az.arena.icearena.service.UserAdminService;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final Set<RoleName> STAFF_ROLES = Set.of(
            RoleName.ROLE_VOLUNTEER,
            RoleName.ROLE_CASHIER,
            RoleName.ROLE_COORDINATOR
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;
    private final VolunteerShiftRepository volunteerShiftRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserAdminDto> listUsers(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(user -> needle.isEmpty()
                        || contains(user.getUsername(), needle)
                        || contains(user.getEmail(), needle)
                        || contains(user.getFirstName(), needle)
                        || contains(user.getLastName(), needle))
                .sorted(Comparator.comparing(User::getId))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAdminDto> listStaff(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(this::isStaffMember)
                .filter(user -> needle.isEmpty()
                        || contains(user.getUsername(), needle)
                        || contains(user.getEmail(), needle)
                        || contains(user.getFirstName(), needle)
                        || contains(user.getLastName(), needle))
                .sorted(Comparator.comparing(User::getId))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminDto getUser(Long id) {
        return toDto(findUser(id));
    }

    @Override
    @Transactional
    public UserAdminDto createStaff(CreateStaffRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Bu istifadəçi adı artıq mövcuddur.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Bu e-poçt artıq istifadə olunur.");
        }

        Set<Role> roles = resolveRoles(request.getRoles());
        roles.add(findRole(RoleName.ROLE_USER));

        String phone = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                ? request.getPhoneNumber().trim()
                : null;

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(phone)
                .roles(roles)
                .build();

        return toDto(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserAdminDto createStaffMember(CreateStaffRequest request) {
        Set<RoleName> requestedRoles = request.getRoles();
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new BadRequestException("Ən azı bir əməkdaş rolu seçilməlidir.");
        }
        boolean hasStaffRole = requestedRoles.stream().anyMatch(STAFF_ROLES::contains);
        if (!hasStaffRole) {
            throw new BadRequestException(
                    "Əməkdaş hesabı üçün VOLUNTEER, CASHIER və ya COORDINATOR rolu tələb olunur.");
        }
        if (requestedRoles.stream().anyMatch(role -> !STAFF_ROLES.contains(role))) {
            throw new BadRequestException(
                    "Bu endpoint yalnız VOLUNTEER, CASHIER və COORDINATOR rolları ilə əməkdaş yaradır.");
        }
        return createStaff(request);
    }

    @Override
    @Transactional
    public UserAdminDto updateStaffRole(Long id, RoleName roleName) {
        if (roleName == null || !STAFF_ROLES.contains(roleName)) {
            throw new BadRequestException(
                    "Yalnız VOLUNTEER, CASHIER və ya COORDINATOR roluna keçid mümkündür.");
        }
        User user = findUser(id);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (isAdmin) {
            throw new BadRequestException("Admin hesabının rolu bu endpoint ilə dəyişdirilə bilməz.");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(findRole(RoleName.ROLE_USER));
        roles.add(findRole(roleName));
        user.setRoles(roles);
        return toDto(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserAdminDto updateStaffMember(Long id, UpdateStaffRequest request) {
        User user = findUser(id);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (isAdmin) {
            throw new BadRequestException("Admin hesabı bu bölmədən redaktə edilə bilməz.");
        }

        String nextUsername = request.getUsername().trim();
        if (!nextUsername.equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsername(nextUsername)) {
            throw new BadRequestException("Bu istifadəçi adı artıq mövcuddur.");
        }

        String nextEmail = request.getEmail().trim();
        if (!nextEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(nextEmail)) {
            throw new BadRequestException("Bu e-poçt ünvanı artıq istifadə olunur.");
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setUsername(nextUsername);
        user.setEmail(nextEmail);
        user.setPhoneNumber(request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                ? request.getPhoneNumber().trim()
                : null);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().trim().length() < 6) {
                throw new BadRequestException("Şifrə ən azı 6 simvol olmalıdır.");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (request.getRole() != null) {
            if (!STAFF_ROLES.contains(request.getRole())) {
                throw new BadRequestException("Yalnız VOLUNTEER, CASHIER və COORDINATOR rolları seçilə bilər.");
            }
            Set<Role> roles = new HashSet<>();
            roles.add(findRole(RoleName.ROLE_USER));
            roles.add(findRole(request.getRole()));
            user.setRoles(roles);
        }

        return toDto(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void deleteStaffMember(Long id, String actorUsername) {
        User user = findUser(id);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (isAdmin || isSelf(user, actorUsername)) {
            throw new BadRequestException("Admin hesabı silinə bilməz.");
        }

        var shifts = volunteerShiftRepository.findByUserIdOrderBySelectedAtDesc(id);
        if (!shifts.isEmpty()) {
            volunteerShiftRepository.deleteAll(shifts);
            volunteerShiftRepository.flush();
        }

        boolean hasHistory = !ticketRepository.findByUserId(id).isEmpty()
                || !reservationRepository.findByUserId(id).isEmpty()
                || (user.getPayments() != null && !user.getPayments().isEmpty())
                || (user.getSkateRentals() != null && !user.getSkateRentals().isEmpty());

        if (!hasHistory) {
            user.getRoles().clear();
            userRepository.saveAndFlush(user);
            userRepository.delete(user);
            userRepository.flush();
        } else {
            user.getRoles().clear();
            String suffix = "_del_" + id + "_" + (System.currentTimeMillis() % 10000);
            String oldUsername = user.getUsername() == null ? "user" : user.getUsername();
            user.setUsername(oldUsername.substring(0, Math.min(oldUsername.length(), 30)) + suffix);
            String oldEmail = user.getEmail() == null ? "deleted@local" : user.getEmail();
            user.setEmail(oldEmail.substring(0, Math.min(oldEmail.length(), 35)) + suffix);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.saveAndFlush(user);
        }
    }

    @Override
    @Transactional
    public UserAdminDto updateRoles(Long id, UserRoleUpdateRequest request, String actorUsername) {
        User user = findUser(id);
        Set<Role> roles = resolveRoles(request.getRoles());
        if (roles.isEmpty()) {
            throw new BadRequestException("İstifadəçinin ən azı bir rolu olmalıdır.");
        }
        if (isSelf(user, actorUsername) && roles.stream()
                .noneMatch(role -> role.getName() == RoleName.ROLE_ADMIN)) {
            throw new BadRequestException("Öz admin rolunuzu silə bilməzsiniz.");
        }
        user.setRoles(roles);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserAdminDto addRole(Long id, RoleName roleName) {
        User user = findUser(id);
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(findRole(roleName));
        user.setRoles(roles);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserAdminDto removeRole(Long id, RoleName roleName, String actorUsername) {
        User user = findUser(id);
        if (isSelf(user, actorUsername) && roleName == RoleName.ROLE_ADMIN) {
            throw new BadRequestException("Öz admin rolunuzu silə bilməzsiniz.");
        }
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.removeIf(role -> role.getName() == roleName);
        if (roles.isEmpty()) {
            throw new BadRequestException("İstifadəçinin ən azı bir rolu olmalıdır.");
        }
        user.setRoles(roles);
        return toDto(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + id));
    }

    private Role findRole(RoleName roleName) {
        if (roleName == null) {
            throw new BadRequestException("Rol tələb olunur.");
        }
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol tapılmadı: " + roleName));
    }

    private Set<Role> resolveRoles(Set<RoleName> requested) {
        Set<Role> roles = new HashSet<>();
        if (requested == null) {
            return roles;
        }
        for (RoleName roleName : requested) {
            if (roleName != null) {
                roles.add(findRole(roleName));
            }
        }
        return roles;
    }

    private UserAdminDto toDto(User user) {
        return UserAdminDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .sorted(Comparator.comparing(RoleName::name))
                        .toList())
                .ticketCount(ticketRepository.findByUserId(user.getId()).size())
                .reservationCount(reservationRepository.findByUserId(user.getId()).size())
                .build();
    }

    private boolean isStaffMember(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(STAFF_ROLES::contains);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean isSelf(User user, String actorUsername) {
        return actorUsername != null && actorUsername.equals(user.getUsername());
    }
}
