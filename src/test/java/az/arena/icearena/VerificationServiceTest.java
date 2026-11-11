package az.arena.icearena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import az.arena.icearena.dto.admin.ActiveGuestDto;
import az.arena.icearena.dto.admin.ActiveGuestsDto;
import az.arena.icearena.dto.admin.VerificationResultDto;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.ReservationActivationService;
import az.arena.icearena.service.VerificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VerificationServiceTest {

    @Autowired
    private VerificationService verificationService;
    @Autowired
    private ReservationActivationService activationService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SkateRepository skateRepository;

    @Test
    void scanIsOneTimeAndHandoverDeductsStockOnlyOnce() {
        User user = userRepository.save(User.builder()
                .username("verification-test-user")
                .email("verification-test-user@icearena.az")
                .password("x")
                .firstName("Verification")
                .lastName("Test")
                .build());
        Session session = sessionRepository.save(Session.builder()
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(23, 0))
                .maxTicketLimit(100)
                .availableTickets(100)
                .blocked(false)
                .build());
        Skate skate = skateRepository.save(Skate.builder()
                .size(998)
                .totalQuantity(10)
                .availableQuantity(10)
                .damagedQuantity(0)
                .build());

        String voucher = "VERIFICATION-TEST-GROUP";
        reservationRepository.saveAll(List.of(
                reservation(user, session, skate, voucher, "Aysel", 22),
                reservation(user, session, skate, voucher, "Murad", 19)
        ));

        VerificationResultDto beforeScan = verificationService.handover(voucher, "volunteer");
        assertFalse(beforeScan.isValid());
        assertEquals("NOT_CHECKED_IN", beforeScan.getStatus());
        assertEquals(10, available(skate));

        VerificationResultDto scanned = verificationService.verify(voucher, "volunteer");
        assertTrue(scanned.isValid());
        assertEquals("CHECKED_IN", scanned.getStatus());
        assertEquals(2, scanned.getParticipantCount());
        assertEquals(List.of("998", "998"), scanned.getSkateSizes());
        assertEquals(List.of("Aysel", "Murad"), scanned.getParticipants().stream()
                .map(participant -> participant.getName())
                .toList());
        assertTrue(scanned.isCanHandover());
        assertNotNull(scanned.getCheckedInAt());
        assertEquals(10, available(skate));

        VerificationResultDto secondScan = verificationService.verify(voucher, "volunteer");
        assertFalse(secondScan.isValid());
        assertEquals("ALREADY_USED", secondScan.getStatus());
        assertEquals(10, available(skate));

        VerificationResultDto handedOut = verificationService.handover(voucher, "volunteer");
        assertTrue(handedOut.isValid());
        assertEquals("HANDED_OUT", handedOut.getStatus());
        assertTrue(handedOut.isSkateHandedOut());
        assertTrue(handedOut.isSkateStockDeducted());
        assertFalse(handedOut.isCanHandover());
        assertEquals(8, available(skate));

        VerificationResultDto secondHandover = verificationService.handover(voucher, "volunteer");
        assertFalse(secondHandover.isValid());
        assertEquals("ALREADY_HANDED_OUT", secondHandover.getStatus());
        assertEquals(8, available(skate));

        VerificationResultDto scanAfterHandover = verificationService.verify(voucher, "volunteer");
        assertFalse(scanAfterHandover.isValid());
        assertEquals("ALREADY_USED", scanAfterHandover.getStatus());
        assertEquals(8, available(skate));
    }

    @Test
    void expiredActivationRestoresStockAndHandoverDeductsItAgain() {
        User user = userRepository.save(User.builder()
                .username("verification-expiry-user")
                .email("verification-expiry-user@icearena.az")
                .password("x")
                .firstName("Expiry")
                .lastName("Test")
                .build());
        Session session = sessionRepository.save(Session.builder()
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(23, 0))
                .maxTicketLimit(100)
                .availableTickets(100)
                .blocked(false)
                .build());
        Skate skate = skateRepository.save(Skate.builder()
                .size(997)
                .totalQuantity(5)
                .availableQuantity(5)
                .damagedQuantity(0)
                .build());

        String voucher = "VERIFICATION-EXPIRY-GROUP";
        Reservation reservation = reservationRepository.save(
                reservation(user, session, skate, voucher, "Expiry Test", 22));

        activationService.activateByCode(voucher, user.getUsername(), false);
        assertEquals(4, available(skate));

        reservation.setTimerExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(reservation);

        VerificationResultDto beforeScan = verificationService.handover(voucher, "volunteer");
        assertFalse(beforeScan.isValid());
        assertEquals("NOT_CHECKED_IN", beforeScan.getStatus());
        // Handover əvvəlcə vaxtı keçmiş aktivasiyanın stokunu geri qaytarır.
        assertEquals(5, available(skate));

        VerificationResultDto scanned = verificationService.verify(voucher, "volunteer");
        assertTrue(scanned.isValid());
        assertEquals("CHECKED_IN", scanned.getStatus());
        assertEquals(5, available(skate));

        VerificationResultDto handedOut = verificationService.handover(voucher, "volunteer");
        assertTrue(handedOut.isValid());
        assertEquals("HANDED_OUT", handedOut.getStatus());
        assertEquals(4, available(skate));
    }

    @Test
    void outOfStockOnAnySizeDoesNotDeductOtherSizes() {
        User user = userRepository.save(User.builder()
                .username("verification-stock-user")
                .email("verification-stock-user@icearena.az")
                .password("x")
                .firstName("Stock")
                .lastName("Test")
                .build());
        Session session = sessionRepository.save(Session.builder()
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(23, 0))
                .maxTicketLimit(100)
                .availableTickets(100)
                .blocked(false)
                .build());
        Skate inStock = skateRepository.save(Skate.builder()
                .size(996)
                .totalQuantity(5)
                .availableQuantity(5)
                .damagedQuantity(0)
                .build());
        Skate outOfStock = skateRepository.save(Skate.builder()
                .size(995)
                .totalQuantity(5)
                .availableQuantity(0)
                .damagedQuantity(0)
                .build());

        String voucher = "VERIFICATION-STOCK-GROUP";
        reservationRepository.saveAll(List.of(
                reservation(user, session, inStock, voucher, "Stok Var", 22),
                reservation(user, session, outOfStock, voucher, "Stok Yox", 20)
        ));

        VerificationResultDto scanned = verificationService.verify(voucher, "volunteer");
        assertTrue(scanned.isValid());
        assertEquals("CHECKED_IN", scanned.getStatus());

        VerificationResultDto handover = verificationService.handover(voucher, "volunteer");
        assertFalse(handover.isValid());
        assertEquals("OUT_OF_STOCK", handover.getStatus());
        // Bir ölçüdə stok çatmadıqda digər ölçülərin stoku azalmamalıdır.
        assertEquals(5, available(inStock));
        assertEquals(0, available(outOfStock));
    }

    @Test
    void activeGuestsSeparatesNotEnteredTicketsFromCustomersOnIce() {
        User user = userRepository.findByUsername("demo").orElseThrow();
        Skate skate = skateRepository.save(Skate.builder()
                .size(994)
                .totalQuantity(5)
                .availableQuantity(5)
                .damagedQuantity(0)
                .build());
        Session session = sessionRepository.save(Session.builder()
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.now().minusMinutes(1))
                .endTime(LocalTime.MAX)
                .maxTicketLimit(100)
                .availableTickets(100)
                .blocked(false)
                .build());

        String voucher = "VERIFICATION-ACTIVE-GROUP";
        Reservation saved = reservationRepository.save(
                reservation(user, session, skate, voucher, "Aktiv Test", 22));
        saved.setEntryTime(LocalTime.now());
        reservationRepository.save(saved);

        ActiveGuestsDto initial = verificationService.activeGuests();
        assertTrue(initial.getActiveTickets().stream()
                .noneMatch(guest -> voucher.equals(guest.getCode())));

        VerificationResultDto cashierScan = verificationService.lookup(voucher, "cashier");
        assertTrue(cashierScan.isValid());
        assertNotNull(cashierScan.getCashierVerifiedAt());

        ActiveGuestsDto before = verificationService.activeGuests();
        assertTrue(before.getActiveTickets().stream()
                .anyMatch(guest -> voucher.equals(guest.getCode())));
        assertTrue(before.getOnIce().stream()
                .noneMatch(guest -> voucher.equals(guest.getCode())));

        VerificationResultDto scan = verificationService.verify(voucher, "volunteer");
        assertTrue(scan.isValid());
        assertEquals("CHECKED_IN", scan.getStatus());
        assertTrue(scan.isActiveOnIce());
        assertTrue(scan.getMinutesRemaining() > 0);

        ActiveGuestsDto after = verificationService.activeGuests();
        ActiveGuestDto onIce = after.getOnIce().stream()
                .filter(guest -> voucher.equals(guest.getCode()))
                .findFirst()
                .orElseThrow();
        assertTrue(onIce.isOnIce());
        assertTrue(onIce.getMinutesRemaining() > 0);
        assertTrue(after.getActiveTickets().stream()
                .noneMatch(guest -> voucher.equals(guest.getCode())));
    }

    private int available(Skate skate) {
        return skateRepository.findById(skate.getId()).orElseThrow().getAvailableQuantity();
    }

    private Reservation reservation(User user, Session session, Skate skate, String voucher,
                                    String participantName, int age) {
        return Reservation.builder()
                .user(user)
                .session(session)
                .skate(skate)
                .status(Reservation.Status.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .durationMinutes(60)
                .reservationExtensionCount(0)
                .skateStockDeducted(false)
                .voucherCode(voucher)
                .participantName(participantName)
                .participantAge(age)
                .build();
    }
}
