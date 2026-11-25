package az.arena.icearena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.ReservationActivationService;
import az.arena.icearena.service.ReservationService;
import az.arena.icearena.service.TicketService;
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
class ReservationActivationServiceTest {

    @Autowired
    private ReservationActivationService activationService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SkateRepository skateRepository;

    @Test
    void activationDeductsStockTwiceAndExpiryRestoresIt() {
        User user = userRepository.save(User.builder()
                .username("activation-test-user")
                .email("activation-test-user@icearena.az")
                .password("x")
                .firstName("Activation")
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
                .size(999)
                .totalQuantity(10)
                .availableQuantity(10)
                .damagedQuantity(0)
                .build());

        String voucher = "ACTIVATION-TEST-GROUP";
        List<Reservation> group = reservationRepository.saveAll(List.of(
                reservation(user, session, skate, voucher),
                reservation(user, session, skate, voucher)
        ));

        List<Reservation> firstActivation = activationService.activateByCode(
                voucher, user.getUsername(), false);
        assertEquals(2, firstActivation.size());
        assertEquals(8, skateRepository.findById(skate.getId()).orElseThrow()
                .getAvailableQuantity());
        assertTrue(firstActivation.stream().allMatch(r ->
                r.getReservationExtensionCount() == 1));
        assertTrue(firstActivation.stream().allMatch(r ->
                Boolean.TRUE.equals(r.getSkateStockDeducted())));
        assertTrue(firstActivation.stream().allMatch(r ->
                r.getTimerExpiresAt() != null
                        && r.getTimerExpiresAt().isAfter(LocalDateTime.now())));

        expireGroup(group);
        assertEquals(10, skateRepository.findById(skate.getId()).orElseThrow()
                .getAvailableQuantity());
        assertTrue(group.stream().allMatch(r ->
                !Boolean.TRUE.equals(r.getSkateStockDeducted())
                        && r.getTimerExpiresAt() == null));

        List<Reservation> secondActivation = activationService.activateByCode(
                voucher, user.getUsername(), false);
        assertEquals(2, secondActivation.size());
        assertEquals(8, skateRepository.findById(skate.getId()).orElseThrow()
                .getAvailableQuantity());
        assertTrue(secondActivation.stream().allMatch(r ->
                r.getReservationExtensionCount() == 2));
        assertNotNull(secondActivation.get(0).getTimerExpiresAt());

        expireGroup(group);
        assertEquals(10, skateRepository.findById(skate.getId()).orElseThrow()
                .getAvailableQuantity());

        assertThrows(BadRequestException.class, () ->
                activationService.activateByCode(voucher, user.getUsername(), false));
        assertEquals(10, skateRepository.findById(skate.getId()).orElseThrow()
                .getAvailableQuantity());
        assertFalse(group.get(0).getSkateStockDeducted());
    }

    @Test
    void cancelAfterHandoutAndReturnWithoutDeductionCannotInflateStock() {
        User user = userRepository.save(User.builder()
                .username("stock-inflation-test-user")
                .email("stock-inflation-test-user@icearena.az")
                .password("x")
                .firstName("Stock")
                .lastName("Guard")
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
                .size(991)
                .totalQuantity(10)
                .availableQuantity(5)
                .damagedQuantity(0)
                .build());

        // 1. Konki təhvil verilib (availableQuantity = 5, 1 ədəd müştəridədir)
        String voucher = "STOCK-GUARD-VOUCHER";
        Reservation res = reservation(user, session, skate, voucher);
        res.setSkateStockDeducted(true);
        res.setSkateHandedOutAt(LocalDateTime.now().minusMinutes(70));
        res.setCheckedInAt(LocalDateTime.now().minusMinutes(65));
        res = reservationRepository.save(res);
        final Long resId = res.getId();

        // Konki götürülmüş rezervasiya /cancel ilə ləğv edilə və stoku artıra bilməz
        assertThrows(BadRequestException.class,
                () -> reservationService.cancelReservation(resId, user.getUsername(), false));
        assertEquals(5, skateRepository.findById(skate.getId()).orElseThrow().getAvailableQuantity());

        // Seans bitdikdən sonra returnSkates stoku yalnız 1 dəfə artırır (5 -> 6)
        ticketService.returnSkates(voucher, user.getUsername());
        assertEquals(6, skateRepository.findById(skate.getId()).orElseThrow().getAvailableQuantity());

        // İkinci dəfə returnSkates çağırılsa rədd edilir və stok dəyişmir (6 qalır)
        assertThrows(BadRequestException.class,
                () -> ticketService.returnSkates(voucher, user.getUsername()));
        assertEquals(6, skateRepository.findById(skate.getId()).orElseThrow().getAvailableQuantity());

        // 2. Stokdan konki çıxılmamış (skateStockDeducted = false) bilet returnSkates ilə stoku şişirdə bilməz
        String noSkateVoucher = "NO-SKATE-DEDUCTED-VOUCHER";
        Reservation noSkateRes = reservation(user, session, skate, noSkateVoucher);
        noSkateRes.setSkateStockDeducted(false);
        noSkateRes.setCheckedInAt(LocalDateTime.now().minusMinutes(65));
        reservationRepository.save(noSkateRes);

        assertThrows(BadRequestException.class,
                () -> ticketService.returnSkates(noSkateVoucher, user.getUsername()));
        assertEquals(6, skateRepository.findById(skate.getId()).orElseThrow().getAvailableQuantity());
    }

    private Reservation reservation(User user, Session session, Skate skate, String voucher) {
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
                .build();
    }

    private void expireGroup(List<Reservation> group) {
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        group.forEach(reservation -> reservation.setTimerExpiresAt(past));
        reservationRepository.saveAll(group);
        activationService.expireStaleReservations();
    }
}
