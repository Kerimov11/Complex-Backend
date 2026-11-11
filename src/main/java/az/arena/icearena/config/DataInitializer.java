// File: src/main/java/az/arena/icearena/config/DataInitializer.java
package az.arena.icearena.config;

import az.arena.icearena.entity.DiscountRule;
import az.arena.icearena.entity.DiscountType;
import az.arena.icearena.entity.Role;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.ShiftSlot;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.repository.DiscountRuleRepository;
import az.arena.icearena.repository.RoleRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.ShiftSlotRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tətbiq ilk dəfə işə düşəndə zəruri məlumatları yaradır:
 *  - rollar (ROLE_USER, ROLE_ADMIN)  -> "Default role not found" xətasının həlli
 *  - demo istifadəçi (userId = 1)
 *  - demo seanslar (sessionId = 1) və konkilər (skateId = 1)
 *
 * H2 in-memory + ddl-auto=create-drop olduğu üçün hər startda bazа sıfırdan qurulur,
 * ona görə bu initializer hər dəfə işləyir. Mövcud məlumat varsa təkrar yazmır.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ShiftSlotRepository shiftSlotRepository;
    private final SkateRepository skateRepository;
    private final DiscountRuleRepository discountRuleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            seedRoles();
            seedDemoUser();
            seedAdminUser();
            seedStaffUsers();
            seedSessions();
            seedShiftSlots();
            seedSkates();
            seedDiscountRules();
            log.info("Data initializer completed: roles={}, users={}, sessions={}, shiftSlots={}, skates={}, discounts={}",
                    roleRepository.count(), userRepository.count(),
                    sessionRepository.count(), shiftSlotRepository.count(),
                    skateRepository.count(), discountRuleRepository.count());
        };
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }
    }

    private void seedDemoUser() {
        User existing = userRepository.findByUsername("demo").orElse(null);
        if (existing != null) {
            if (existing.getDateOfBirth() == null) {
                existing.setDateOfBirth(LocalDate.of(2006, 7, 25));
                existing.setAgeVerifiedAt(java.time.LocalDateTime.now());
                userRepository.save(existing);
            }
            return;
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER seed olunmadı"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User demo = User.builder()
                .username("demo")
                .email("demo@icearena.az")
                .password(passwordEncoder.encode("demo123"))
                .firstName("Demo")
                .lastName("User")
                .phoneNumber("+994501112233")
                .dateOfBirth(LocalDate.of(2006, 7, 25))
                .ageVerifiedAt(java.time.LocalDateTime.now())
                .roles(roles)
                .build();

        userRepository.save(demo);
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER seed olunmadı"));
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN seed olunmadı"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        User admin = User.builder()
                .username("admin")
                .email("admin@icearena.az")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Arena")
                .lastName("Admin")
                .phoneNumber("+994501112244")
                .roles(roles)
                .build();

        userRepository.save(admin);
    }

    /**
     * Demo kassir və könüllü hesabları. Admin panelində bu rollar
     * UserManagement bölməsindən də təyin/ləğv edilə bilər.
     */
    private void seedStaffUsers() {
        seedStaffUser("cashier", "cashier@icearena.az", "cashier123",
                "Arena", "Kassir", "+994501112255",
                RoleName.ROLE_CASHIER);
        seedStaffUser("volunteer", "volunteer@icearena.az", "volunteer123",
                "Arena", "Könüllü", "+994501112266",
                RoleName.ROLE_VOLUNTEER);
        seedStaffUser("coordinator", "coordinator@icearena.az", "coordinator123",
                "Arena", "Koordinator", "+994501112277",
                RoleName.ROLE_COORDINATOR);
    }

    private void seedStaffUser(String username,
                               String email,
                               String password,
                               String firstName,
                               String lastName,
                               String phone,
                               RoleName staffRole) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER seed olunmadı"));
        Role role = roleRepository.findByName(staffRole)
                .orElseThrow(() -> new IllegalStateException(staffRole + " seed olunmadı"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(role);

        userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phone)
                .roles(roles)
                .build());
    }

    /**
     * Frontend-in iş saatları cədvəli ilə eyni aralıqları seed edir.
     * Beləliklə istifadəçinin seçdiyi istənilən 5 dəqiqəlik slot
     * backend-də mövcud bir seansa düşür və rezervasiya yaradıla bilir.
     */
    private void seedSessions() {
        if (sessionRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < 14; offset++) {
            LocalDate date = today.plusDays(offset);
            for (TimeRange range : workingRanges(date.getDayOfWeek())) {
                sessionRepository.save(buildSession(date, range.start(), range.end()));
            }
        }

        log.info("Seeded sessions for 14 days ({} total)", sessionRepository.count());
    }

    private void seedShiftSlots() {
        if (shiftSlotRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < 14; offset++) {
            LocalDate date = today.plusDays(offset);
            for (TimeRange range : defaultShiftRanges(date.getDayOfWeek())) {
                shiftSlotRepository.save(ShiftSlot.builder()
                        .shiftDate(date)
                        .startTime(range.start())
                        .endTime(range.end())
                        .active(true)
                        .createdAt(java.time.LocalDateTime.now())
                        .updatedAt(java.time.LocalDateTime.now())
                        .build());
            }
        }
        log.info("Seeded volunteer shift slots for 14 days ({} total)",
                shiftSlotRepository.count());
    }

    private List<TimeRange> defaultShiftRanges(DayOfWeek day) {
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return List.of(
                    new TimeRange(LocalTime.of(9, 50), LocalTime.of(13, 0)),
                    new TimeRange(LocalTime.of(13, 0), LocalTime.of(16, 15)),
                    new TimeRange(LocalTime.of(16, 0), LocalTime.of(19, 15)),
                    new TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
            );
        }
        return List.of(
                new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 25)),
                new TimeRange(LocalTime.of(12, 10), LocalTime.of(14, 30)),
                new TimeRange(LocalTime.of(18, 15), LocalTime.of(22, 0))
        );
    }

    private List<TimeRange> workingRanges(DayOfWeek day) {
        return switch (day) {
            case MONDAY, TUESDAY, WEDNESDAY -> List.of(
                    new TimeRange(LocalTime.of(10, 30), LocalTime.of(13, 45)),
                    new TimeRange(LocalTime.of(18, 45), LocalTime.of(21, 30))
            );
            case THURSDAY, FRIDAY -> List.of(
                    new TimeRange(LocalTime.of(10, 0), LocalTime.of(13, 45)),
                    new TimeRange(LocalTime.of(18, 45), LocalTime.of(21, 30))
            );
            case SATURDAY -> List.of(
                    new TimeRange(LocalTime.of(10, 45), LocalTime.of(22, 0))
            );
            case SUNDAY -> List.of(
                    new TimeRange(LocalTime.of(10, 0), LocalTime.of(22, 0))
            );
        };
    }

    private Session buildSession(LocalDate date, LocalTime start, LocalTime end) {
        return Session.builder()
                .sessionDate(date)
                .startTime(start)
                .endTime(end)
                .maxTicketLimit(50)
                .availableTickets(50)
                .build();
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }

    private void seedSkates() {
        if (skateRepository.count() > 0) {
            return;
        }

        for (int size = 36; size <= 45; size++) {
            skateRepository.save(Skate.builder()
                    .size(size)
                    .totalQuantity(20)
                    .availableQuantity(20)
                    .build());
        }
    }

    private void seedDiscountRules() {
        if (discountRuleRepository.count() > 0) {
            return;
        }

        List<DiscountRule> rules = List.of(
                rule(DiscountType.GROUP, "Qrup endirimi",
                        "10 nəfər və daha çox 1 saatlıq biletə avtomatik 20% endirim.",
                        new BigDecimal("20.00"), 10, null, 60),
                rule(DiscountType.FIVE_PLUS_ONE, "5+1 kampaniyası",
                        "Hər 6 nəfərdən 1-i pulsuzdur; 12 nəfər = 2 pulsuz.",
                        null, 6, 1, null),
                rule(DiscountType.STUDENT, "Tələbə endirimi",
                        "Həftəiçi 1 saatlıq biletə 20% endirim.",
                        new BigDecimal("20.00"), 1, null, 60),
                rule(DiscountType.FAMILY, "Ailə endirimi",
                        "2 nəfərdən başlayaraq 1 nəfər pulsuz.",
                        null, 2, 1, null),
                rule(DiscountType.PUSH30, "PUSH30",
                        "Premium paket sahibləri üçün 1 saatlıq 1 pulsuz giriş.",
                        null, 1, 1, 60),
                rule(DiscountType.QAZI, "Qazi güzəşti",
                        "İlk giriş pulsuz, 2-ci giriş 50% endirim.",
                        new BigDecimal("50.00"), 1, 1, 60),
                rule(DiscountType.SHAHEED, "Şəhid ailəsi",
                        "Təsdiqlənmiş şəhid ailəsi üçün pulsuz giriş.",
                        null, 1, 50, null),
                rule(DiscountType.SUNDAY_MORNING, "Bazar səhəri endirimi",
                        "Bazar günü 10:00–12:00 arası 1 saatlıq biletə 20% endirim.",
                        new BigDecimal("20.00"), 1, null, 60)
        );
        discountRuleRepository.saveAll(rules);
    }

    private DiscountRule rule(DiscountType type,
                              String title,
                              String description,
                              BigDecimal percent,
                              Integer minParticipants,
                              Integer freeTickets,
                              Integer durationMinutes) {
        return DiscountRule.builder()
                .type(type)
                .title(title)
                .description(description)
                .percent(percent)
                .minParticipants(minParticipants)
                .freeTickets(freeTickets)
                .durationMinutes(durationMinutes)
                .active(true)
                .build();
    }
}
