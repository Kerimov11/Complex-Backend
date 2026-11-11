package az.arena.icearena.service.impl;

import az.arena.icearena.dto.admin.ActiveGuestDto;
import az.arena.icearena.dto.admin.ActiveGuestsDto;
import az.arena.icearena.dto.admin.VerificationParticipantDto;
import az.arena.icearena.dto.admin.VerificationResultDto;
import az.arena.icearena.entity.Payment;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.Ticket;
import az.arena.icearena.entity.User;
import az.arena.icearena.repository.PaymentRepository;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SkateRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.service.ReservationActivationService;
import az.arena.icearena.service.VerificationService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final SkateRepository skateRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationActivationService reservationActivationService;

    @Override
    @Transactional
    public VerificationResultDto verify(String code, String username) {
        String normalized = normalize(code);
        if (normalized == null) {
            return invalid(code, "Kod boşdur.");
        }

        List<Reservation> reservations = findReservations(code);
        if (!reservations.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();

            if (reservations.stream().anyMatch(r -> r.getCheckedInAt() != null)) {
                Reservation checked = reservations.stream()
                        .filter(r -> r.getCheckedInAt() != null)
                        .findFirst()
                        .orElse(reservations.get(0));
                String checkTime = checked.getCheckedInAt() != null
                        ? checked.getCheckedInAt().toLocalTime().toString().substring(0, 5) : "";
                return reservationBuilder(reservations, normalized)
                        .valid(false)
                        .status("ALREADY_USED")
                        .message("Bu QR bilet artıq turniketdə istifadə olunub (" + checkTime + "). Təkrar turniketdə skan edilə bilməz.")
                        .checkedInAt(checked.getCheckedInAt())
                        .checkedInBy(checked.getCheckedInBy())
                        .build();
            }

            for (Reservation reservation : reservations) {
                reservation.setStatus(Reservation.Status.CONFIRMED);
                if (reservation.getCashierVerifiedAt() == null) {
                    reservation.setCashierVerifiedAt(now);
                    reservation.setCashierVerifiedBy(username);
                }
                reservation.setCheckedInAt(now);
                reservation.setCheckedInBy(username);
                reservationRepository.save(reservation);
            }

            return reservationBuilder(reservations, normalized)
                    .valid(true)
                    .status("CHECKED_IN")
                    .message("Bilet turniketdə uğurla təsdiqləndi! Müştərini buza buraxın.")
                    .checkedInAt(now)
                    .checkedInBy(username)
                    .build();
        }

        Optional<Ticket> ticket = findTicket(normalized, true);
        if (ticket.isPresent()) {
            return verifyTicket(ticket.get(), normalized, username);
        }

        return invalid(code, "Bu koda uyğun bilet tapılmadı.");
    }

    @Override
    @Transactional
    public VerificationResultDto lookup(String code) {
        return lookup(code, null);
    }

    @Override
    @Transactional
    public VerificationResultDto lookup(String code, String username) {
        String normalized = normalize(code);
        if (normalized == null) {
            return invalid(code, "Kod boşdur.");
        }
        List<Reservation> reservations = findReservations(code);
        if (!reservations.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();

            // 1. Artıq turniketdə istifadə olunubsa
            if (reservations.stream().anyMatch(r -> r.getCheckedInAt() != null)) {
                Reservation checked = reservations.stream()
                        .filter(r -> r.getCheckedInAt() != null)
                        .findFirst()
                        .orElse(reservations.get(0));
                String checkTime = checked.getCheckedInAt() != null
                        ? checked.getCheckedInAt().toLocalTime().toString().substring(0, 5) : "";
                return reservationBuilder(reservations, normalized)
                        .valid(false)
                        .status("ALREADY_USED")
                        .message("Bu QR bilet artıq turniketdən keçib / istifadə olunub (" + checkTime + ").")
                        .checkedInAt(checked.getCheckedInAt())
                        .checkedInBy(checked.getCheckedInBy())
                        .build();
            }

            // 2. Artıq kassada təsdiqlənibsə (kassada yalnız 1 dəfə skan oluna bilər!)
            if (reservations.stream().anyMatch(r -> r.getCashierVerifiedAt() != null)) {
                Reservation verified = reservations.stream()
                        .filter(r -> r.getCashierVerifiedAt() != null)
                        .findFirst()
                        .orElse(reservations.get(0));
                String verifiedTime = verified.getCashierVerifiedAt() != null
                        ? verified.getCashierVerifiedAt().toLocalTime().toString().substring(0, 5) : "";
                return reservationBuilder(reservations, normalized)
                        .valid(false)
                        .status("ALREADY_CASHIER_VERIFIED")
                        .message("Bu bilet artıq kassada təsdiqlənib (" + verifiedTime + "). Təkrar kassada skan edilə bilməz.")
                        .build();
            }

            // 3. İlk dəfə kassada skan edilir: təsdiqlə və Aktiv biletlər siyahısına sal!
            for (Reservation reservation : reservations) {
                reservation.setStatus(Reservation.Status.CONFIRMED);
                reservation.setCashierVerifiedAt(now);
                reservation.setCashierVerifiedBy(username);
                reservationRepository.save(reservation);
            }
            return reservationBuilder(reservations, normalized)
                    .valid(true)
                    .status("VALID")
                    .message("Bilet kassada təsdiqləndi və Aktiv bilet siyahısına əlavə olundu.")
                    .cashierVerifiedAt(now)
                    .cashierVerifiedBy(username)
                    .build();
        }

        Optional<Ticket> ticket = findTicket(normalized, true);
        if (ticket.isPresent()) {
            Ticket found = ticket.get();
            VerificationResultDto check = lookupTicket(found, normalized);

            // 1. Turniketdə istifadə olunubsa
            if (Boolean.TRUE.equals(found.getIsUsed()) || found.getCheckedInAt() != null) {
                check.setValid(false);
                check.setStatus("ALREADY_USED");
                check.setMessage("Bu QR bilet artıq turniketdən keçib / istifadə olunub.");
                return check;
            }

            // 2. Kassada artıq təsdiqlənibsə
            if (found.getCashierVerifiedAt() != null) {
                check.setValid(false);
                check.setStatus("ALREADY_CASHIER_VERIFIED");
                check.setMessage("Bu bilet artıq kassada təsdiqlənib. Təkrar kassada skan edilə bilməz.");
                return check;
            }

            // 3. İlk dəfə kassada təsdiqlənir
            LocalDateTime now = LocalDateTime.now();
            found.setCashierVerifiedAt(now);
            found.setCashierVerifiedBy(username);
            ticketRepository.save(found);
            check.setValid(true);
            check.setStatus("VALID");
            check.setMessage("Bilet kassada təsdiqləndi və Aktiv bilet siyahısına əlavə olundu.");
            check.setCashierVerifiedAt(now);
            check.setCashierVerifiedBy(username);
            return check;
        }
        return invalid(code, "Bu koda uyğun bilet tapılmadı.");
    }

    private List<Reservation> findReservations(String code) {
        if (code == null || code.isBlank()) {
            return List.of();
        }
        String clean = code.trim();

        // 1. Dəqiq voucherCode ilə axtarış (böyük-kiçik hərf fərq etmədən)
        List<Reservation> list = reservationRepository.findByVoucherCodeIgnoreCase(clean);
        if (!list.isEmpty()) {
            return list;
        }

        // 2. Əgər QR kod daxilində URL və ya prefiks varsa (məs. SAV-XXX-XXXX formatı)
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(SAV-[A-Z0-9]+-[A-Z0-9]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(clean);
        if (m.find()) {
            list = reservationRepository.findByVoucherCodeIgnoreCase(m.group(1));
            if (!list.isEmpty()) {
                return list;
            }
        }

        // 3. RES-123 və ya birbaşa ID ilə axtarış
        String upper = clean.toUpperCase(Locale.ROOT);
        if (upper.startsWith("RES-")) {
            try {
                Long id = Long.parseLong(upper.substring(4).trim());
                Optional<Reservation> r = reservationRepository.findById(id);
                if (r.isPresent()) {
                    return List.of(r.get());
                }
            } catch (NumberFormatException ignored) {}
        }
        try {
            Long id = Long.parseLong(clean);
            Optional<Reservation> r = reservationRepository.findById(id);
            if (r.isPresent()) {
                return List.of(r.get());
            }
        } catch (NumberFormatException ignored) {}

        return List.of();
    }

    /**
     * Turniket skanından sonra konkini müştəriyə təhvil verir.
     * Stok yalnız bu düymə basılanda, hələ çıxılmayıbsa, çıxılır.
     * Eyni bilet üçün ikinci dəfə stok çıxılmır.
     */
    @Override
    @Transactional
    public VerificationResultDto handover(String code, String username) {
        String normalized = normalize(code);
        if (normalized == null) {
            return invalid(code, "Kod boşdur.");
        }

        reservationActivationService.expireStaleReservations();

        List<Reservation> reservations = findReservations(code);
        if (reservations.isEmpty()) {
            return invalid(code, "Bu koda uyğun onlayn bilet tapılmadı.");
        }

        if (reservations.stream().anyMatch(r -> r.getSkateHandedOutAt() != null)) {
            return reservationBuilder(reservations, normalized)
                    .valid(false)
                    .status("ALREADY_HANDED_OUT")
                    .message("Bu biletin konkiləri artıq təhvil verilib.")
                    .build();
        }

        if (reservations.stream().anyMatch(r -> r.getCashierVerifiedAt() == null && r.getCheckedInAt() == null)) {
            return reservationBuilder(reservations, normalized)
                    .valid(false)
                    .status("NOT_CHECKED_IN")
                    .message("Əvvəlcə Kassa QR və ya Turniketdə QR kodu skan edib bileti təsdiqləyin.")
                    .build();
        }

        if (reservations.stream().anyMatch(r -> r.getStatus() != Reservation.Status.CONFIRMED)) {
            return reservationBuilder(reservations, normalized)
                    .valid(false)
                    .status("PAYMENT_PENDING")
                    .message("Bilet ödənişlə təsdiqlənmədiyi üçün konki təhvil verilə bilməz.")
                    .build();
        }

        Map<Long, Integer> requiredBySkate = new LinkedHashMap<>();
        for (Reservation reservation : reservations) {
            if (Boolean.TRUE.equals(reservation.getSkateStockDeducted())) {
                continue;
            }
            Skate skate = reservation.getSkate();
            if (skate == null || skate.getId() == null) {
                return reservationBuilder(reservations, normalized)
                        .valid(false)
                        .status("NOT_FOUND")
                        .message("Rezervasiyada konki ölçüsü göstərilməyib.")
                        .build();
            }
            requiredBySkate.merge(skate.getId(), 1, Integer::sum);
        }

        List<Map.Entry<Long, Integer>> lockedInOrder = requiredBySkate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        Map<Long, Skate> lockedSkates = new LinkedHashMap<>();
        // Əvvəlcə bütün ölçüləri yoxla, sonra stoku çıx. Beləliklə, ikinci
        // ölçüdə stok çatmadıqda birinci ölçünün stoku yarımçıq azalmır.
        for (Map.Entry<Long, Integer> entry : lockedInOrder) {
            Skate skate = skateRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new IllegalStateException(
                            "Konki tapılmadı: " + entry.getKey()));
            int available = skate.getAvailableQuantity() == null ? 0 : skate.getAvailableQuantity();
            if (available < entry.getValue()) {
                return reservationBuilder(reservations, normalized)
                        .valid(false)
                        .status("OUT_OF_STOCK")
                        .message("Bu ölçüdə kifayət qədər konki yoxdur (ölçü "
                                + skate.getDisplaySize() + ", tələb " + entry.getValue()
                                + ", mövcud " + available + ").")
                        .build();
            }
            lockedSkates.put(entry.getKey(), skate);
        }
        for (Map.Entry<Long, Integer> entry : lockedInOrder) {
            Skate skate = lockedSkates.get(entry.getKey());
            int available = skate.getAvailableQuantity() == null ? 0 : skate.getAvailableQuantity();
            skate.setAvailableQuantity(available - entry.getValue());
            skateRepository.save(skate);
        }

        LocalDateTime now = LocalDateTime.now();
        for (Reservation reservation : reservations) {
            reservation.setSkateStockDeducted(true);
            reservation.setSkateHandedOutAt(now);
            reservation.setSkateHandedOutBy(username);
            reservation.setTimerExpiresAt(null);
            reservationRepository.save(reservation);
        }

        return reservationBuilder(reservations, normalized)
                .valid(true)
                .status("HANDED_OUT")
                .message("Konki təhvil verildi və stokdan çıxıldı. Bu əməliyyat təkrar edilə bilməz.")
                .build();
    }

    /**
     * Xüsusi panellər üçün iki ayrı siyahı:
     *  - aktivTickets: hələ turniketdən keçməyən etibarlı biletlər;
     *  - onIce: turniketdə təsdiqlənib hazırda buzda olan müştərilər.
     */
    @Override
    @Transactional(readOnly = true)
    public ActiveGuestsDto activeGuests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<ActiveGuestDto> activeTickets = new ArrayList<>();
        List<ActiveGuestDto> onIce = new ArrayList<>();

        List<Reservation> reservations = reservationRepository
                .findByStatusInAndSessionSessionDateGreaterThanEqualOrderBySessionSessionDateAscSessionStartTimeAscIdAsc(
                        List.of(Reservation.Status.CONFIRMED), today.minusDays(1));
        Map<String, List<Reservation>> groups = new LinkedHashMap<>();
        for (Reservation reservation : reservations) {
            String code = reservation.getVoucherCode();
            if (code == null || code.isBlank()) {
                code = "RES-" + reservation.getId();
            }
            groups.computeIfAbsent(code, key -> new ArrayList<>()).add(reservation);
        }
        for (Map.Entry<String, List<Reservation>> entry : groups.entrySet()) {
            List<Reservation> group = entry.getValue();
            VerificationResultDto result = reservationBuilder(group, entry.getKey()).build();
            boolean checkedIn = group.stream().anyMatch(r -> r.getCheckedInAt() != null);
            boolean cashierVerified = group.stream().anyMatch(r -> r.getCashierVerifiedAt() != null);
            if (checkedIn) {
                if (result.isActiveOnIce()) {
                    onIce.add(toActiveGuest(result, true));
                }
            } else if (cashierVerified) {
                Session session = group.get(0).getSession();
                LocalDateTime sessionEnd = sessionEnd(session);
                if (session == null || session.getSessionDate() == null
                        || !session.getSessionDate().isBefore(today)
                        && (sessionEnd == null || sessionEnd.isAfter(now))) {
                    activeTickets.add(toActiveGuest(result, false));
                }
            }
        }

        for (Ticket ticket : ticketRepository.findAll()) {
            VerificationResultDto result = lookupTicket(ticket, ticket.getTicketCode());
            if (Boolean.TRUE.equals(ticket.getIsUsed())) {
                if (result.isActiveOnIce()) {
                    onIce.add(toActiveGuest(result, true));
                }
            } else if (ticket.getCashierVerifiedAt() != null) {
                Session session = ticket.getSession();
                LocalDateTime sessionEnd = sessionEnd(session);
                if (session == null || session.getSessionDate() == null
                        || !session.getSessionDate().isBefore(today)
                        && (sessionEnd == null || sessionEnd.isAfter(now))) {
                    activeTickets.add(toActiveGuest(result, false));
                }
            }
        }

        activeTickets.sort(Comparator
                .comparing(ActiveGuestDto::getCashierVerifiedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ActiveGuestDto::getSessionDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ActiveGuestDto::getSessionStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        onIce.sort(Comparator
                .comparing(ActiveGuestDto::getCheckedInAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(ActiveGuestDto::getMinutesRemaining));

        return ActiveGuestsDto.builder()
                .activeTickets(activeTickets)
                .onIce(onIce)
                .build();
    }

    private VerificationResultDto validateReservations(List<Reservation> reservations, String code) {
        Reservation first = reservations.get(0);
        Session session = first.getSession();
        VerificationResultDto.VerificationResultDtoBuilder builder =
                reservationBuilder(reservations, code);

        if (reservations.stream().anyMatch(r -> r.getCheckedInAt() != null)) {
            Reservation checked = reservations.stream()
                    .filter(r -> r.getCheckedInAt() != null)
                    .findFirst()
                    .orElse(first);
            return builder.valid(false).status("ALREADY_USED")
                    .message("Bu QR bilet istifadə olunub.")
                    .checkedInAt(checked.getCheckedInAt())
                    .checkedInBy(checked.getCheckedInBy())
                    .build();
        }
        // Bilet skan edilənədək limitsiz etibarlıdır (müddət bitmir)
        boolean hasPending = reservations.stream()
                .anyMatch(r -> r.getStatus() == Reservation.Status.PENDING);
        if (hasPending) {
            return builder.valid(false).status("PAYMENT_PENDING")
                    .message("Rezervasiya hələ ödənişlə təsdiqlənməyib.").build();
        }
        boolean hasConfirmed = reservations.stream()
                .anyMatch(r -> r.getStatus() == Reservation.Status.CONFIRMED);
        if (!hasConfirmed) {
            return builder.valid(false).status("CANCELLED")
                    .message("Bu bilet ləğv edilib və ya vaxtı bitib.").build();
        }
        return builder.valid(true).status("VALID")
                .message("Bilet etibarlıdır; hələ təsdiqlənməyib.").build();
    }

    private VerificationResultDto.VerificationResultDtoBuilder reservationBuilder(
            List<Reservation> reservations, String code) {
        Reservation first = reservations.get(0);
        Session session = first.getSession();
        User user = first.getUser();
        Payment payment = findPaymentForReservations(reservations);

        List<String> sizes = reservations.stream()
                .map(Reservation::getSkate)
                .filter(skate -> skate != null && skate.getSize() != null)
                .sorted(java.util.Comparator.comparing(az.arena.icearena.entity.Skate::getSize))
                .map(az.arena.icearena.entity.Skate::getDisplaySize)
                .toList();

        String ownerName = fullName(user);
        List<VerificationParticipantDto> participants = new ArrayList<>();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            Skate skate = reservation.getSkate();
            String name = reservation.getParticipantName();
            if (name == null || name.isBlank()) {
                if (ownerName != null && !ownerName.isBlank()) {
                    name = i == 0 ? ownerName : ownerName + " (Qrup üzvü " + (i + 1) + ")";
                } else {
                    name = "İştirakçı " + (i + 1);
                }
            }
            participants.add(VerificationParticipantDto.builder()
                    .reservationId(reservation.getId())
                    .name(name)
                    .age(reservation.getParticipantAge())
                    .skateSize(skate == null ? null : skate.getDisplaySize())
                    .skateStockDeducted(Boolean.TRUE.equals(reservation.getSkateStockDeducted()))
                    .skateHandedOut(reservation.getSkateHandedOutAt() != null)
                    .skateHandedOutAt(reservation.getSkateHandedOutAt())
                    .skateHandedOutBy(reservation.getSkateHandedOutBy())
                    .build());
        }

        boolean allCashierVerified = reservations.stream()
                .allMatch(r -> r.getCashierVerifiedAt() != null);
        boolean allCheckedIn = reservations.stream()
                .allMatch(r -> r.getCheckedInAt() != null);
        boolean anyHandedOut = reservations.stream()
                .anyMatch(r -> r.getSkateHandedOutAt() != null);
        boolean allHandedOut = reservations.stream()
                .allMatch(r -> r.getSkateHandedOutAt() != null);
        boolean allStockDeducted = reservations.stream()
                .allMatch(r -> Boolean.TRUE.equals(r.getSkateStockDeducted()));
        Reservation cashierChecked = reservations.stream()
                .filter(r -> r.getCashierVerifiedAt() != null)
                .findFirst()
                .orElse(null);
        Reservation checked = reservations.stream()
                .filter(r -> r.getCheckedInAt() != null)
                .findFirst()
                .orElse(null);
        Reservation handedOut = reservations.stream()
                .filter(r -> r.getSkateHandedOutAt() != null)
                .findFirst()
                .orElse(null);

        int duration = resolveDuration(first.getDurationMinutes(), session);
        LocalTime entryTime = first.getEntryTime();
        LocalDateTime checkedInAt = checked == null ? null : checked.getCheckedInAt();
        LocalDateTime endsAt = resolveEndsAt(session, entryTime, duration, checkedInAt);
        boolean activeOnIce = isOnIce(session, checkedInAt, endsAt);

        return VerificationResultDto.builder()
                .code(code == null ? null : code.trim())
                .source("RESERVATION")
                .ownerName(ownerName)
                .ownerUsername(user == null ? null : user.getUsername())
                .customerName(ownerName)
                .sessionDate(session == null ? null : session.getSessionDate())
                .sessionStartTime(session == null ? null : session.getStartTime())
                .sessionEndTime(session == null ? null : session.getEndTime())
                .entryTime(entryTime)
                .durationMinutes(duration)
                .participantCount(reservations.size())
                .skateSizes(sizes)
                .participants(participants)
                .reservationIds(reservations.stream().map(Reservation::getId).toList())
                .ticketCode(code == null ? null : code.trim())
                .ticketType("ONLINE")
                .price(payment == null ? null : payment.getAmount())
                .paymentMethod(payment == null || payment.getPaymentMethod() == null
                        ? null : payment.getPaymentMethod().name())
                .purchaseDate(payment == null ? first.getCreatedAt() : payment.getPaymentDate())
                .saleReference(payment == null ? null : payment.getReference())
                .cashierVerifiedAt(cashierChecked == null ? null : cashierChecked.getCashierVerifiedAt())
                .cashierVerifiedBy(cashierChecked == null ? null : cashierChecked.getCashierVerifiedBy())
                .checkedInAt(checkedInAt)
                .checkedInBy(checked == null ? null : checked.getCheckedInBy())
                .activeOnIce(activeOnIce)
                .endsAt(endsAt)
                .minutesRemaining(activeOnIce ? remainingMinutes(endsAt) : 0)
                .minutesUntilSessionEnd(remainingMinutes(sessionEnd(session)))
                .skateHandedOut(allHandedOut && anyHandedOut)
                .skateHandedOutAt(handedOut == null ? null : handedOut.getSkateHandedOutAt())
                .skateHandedOutBy(handedOut == null ? null : handedOut.getSkateHandedOutBy())
                .skateStockDeducted(allStockDeducted)
                .canHandover((allCashierVerified || allCheckedIn) && !anyHandedOut)
                .valid(false);
    }

    private VerificationResultDto verifyTicket(Ticket ticket, String code, String username) {
        VerificationResultDto check = lookupTicket(ticket, code);
        if (Boolean.TRUE.equals(ticket.getIsUsed()) || ticket.getCheckedInAt() != null) {
            String checkTime = ticket.getCheckedInAt() != null
                    ? ticket.getCheckedInAt().toLocalTime().toString().substring(0, 5) : "";
            check.setValid(false);
            check.setStatus("ALREADY_USED");
            check.setMessage("Bu QR bilet artıq turniketdə istifadə olunub (" + checkTime + "). Təkrar turniketdə skan edilə bilməz.");
            return check;
        }
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getCashierVerifiedAt() == null) {
            ticket.setCashierVerifiedAt(now);
            ticket.setCashierVerifiedBy(username);
        }
        ticket.setIsUsed(true);
        ticket.setCheckedInAt(now);
        ticket.setCheckedInBy(username);
        ticketRepository.save(ticket);

        int duration = resolveDuration(ticket.getDurationMinutes(), ticket.getSession());
        LocalDateTime endsAt = resolveEndsAt(ticket.getSession(), ticket.getEntryTime(), duration, now);
        boolean active = isOnIce(ticket.getSession(), now, endsAt);
        check.setStatus("CHECKED_IN");
        check.setValid(true);
        check.setMessage("Bilet turniketdə uğurla təsdiqləndi! Müştərini buza buraxın.");
        check.setCashierVerifiedAt(ticket.getCashierVerifiedAt());
        check.setCashierVerifiedBy(ticket.getCashierVerifiedBy());
        check.setCheckedInAt(now);
        check.setCheckedInBy(username);
        check.setEndsAt(endsAt);
        check.setActiveOnIce(active);
        check.setMinutesRemaining(active ? remainingMinutes(endsAt) : 0);
        return check;
    }

    private VerificationResultDto lookupTicket(Ticket ticket, String code) {
        Session session = ticket.getSession();
        User user = ticket.getUser();
        boolean posTicket = ticket.getSoldByUsername() != null
                && !ticket.getSoldByUsername().isBlank();
        String ownerName = posTicket
                ? (ticket.getCustomerName() == null || ticket.getCustomerName().isBlank()
                ? "Kassa müştərisi" : ticket.getCustomerName())
                : fullName(user);
        String ownerUsername = posTicket ? null : (user == null ? null : user.getUsername());
        int duration = resolveDuration(ticket.getDurationMinutes(), session);
        LocalTime entryTime = ticket.getEntryTime();
        if (entryTime == null && ticket.getPurchaseDate() != null && session != null
                && session.getSessionDate() != null
                && ticket.getPurchaseDate().toLocalDate().equals(session.getSessionDate())) {
            entryTime = ticket.getPurchaseDate().toLocalTime();
        }
        if (entryTime == null && session != null) {
            entryTime = session.getStartTime();
        }
        LocalDateTime checkedInAt = ticket.getCheckedInAt();
        if (checkedInAt == null && Boolean.TRUE.equals(ticket.getIsUsed())) {
            checkedInAt = ticket.getPurchaseDate();
        }
        LocalDateTime endsAt = resolveEndsAt(session, entryTime, duration, checkedInAt);
        boolean activeOnIce = isOnIce(session, checkedInAt, endsAt);
        List<VerificationParticipantDto> participants = List.of(
                VerificationParticipantDto.builder()
                        .name(ownerName == null ? "Kassa müştərisi" : ownerName)
                        .build());

        VerificationResultDto.VerificationResultDtoBuilder builder = baseBuilder(code)
                .source("TICKET")
                .ownerName(ownerName)
                .ownerUsername(ownerUsername)
                .customerName(ownerName)
                .sessionDate(session == null ? null : session.getSessionDate())
                .sessionStartTime(session == null ? null : session.getStartTime())
                .sessionEndTime(session == null ? null : session.getEndTime())
                .entryTime(entryTime)
                .durationMinutes(duration)
                .participantCount(1)
                .skateSizes(List.of())
                .participants(participants)
                .ticketCode(ticket.getTicketCode())
                .ticketType(ticket.getTicketType() == null ? null : ticket.getTicketType().name())
                .price(ticket.getPrice())
                .paymentMethod(ticket.getPaymentMethod() == null
                        ? null : ticket.getPaymentMethod().name())
                .purchaseDate(ticket.getPurchaseDate())
                .soldByUsername(ticket.getSoldByUsername())
                .saleReference(ticket.getSaleReference())
                .cashierVerifiedAt(ticket.getCashierVerifiedAt())
                .cashierVerifiedBy(ticket.getCashierVerifiedBy())
                .checkedInAt(checkedInAt)
                .checkedInBy(ticket.getCheckedInBy())
                .activeOnIce(activeOnIce)
                .endsAt(endsAt)
                .minutesRemaining(activeOnIce ? remainingMinutes(endsAt) : 0)
                .minutesUntilSessionEnd(remainingMinutes(sessionEnd(session)))
                .canHandover(false);

        if (Boolean.TRUE.equals(ticket.getIsUsed())) {
            return builder.valid(false).status("ALREADY_USED")
                    .message("Bu QR bilet istifadə olunub.")
                    .build();
        }
        return builder.valid(true).status("VALID")
                .message("Bilet etibarlıdır; hələ təsdiqlənməyib.").build();
    }

    private ActiveGuestDto toActiveGuest(VerificationResultDto result, boolean onIce) {
        return ActiveGuestDto.builder()
                .source(result.getSource())
                .code(result.getCode())
                .ticketCode(result.getTicketCode())
                .ownerName(result.getOwnerName())
                .ownerUsername(result.getOwnerUsername())
                .customerName(result.getCustomerName())
                .sessionDate(result.getSessionDate())
                .sessionStartTime(result.getSessionStartTime())
                .sessionEndTime(result.getSessionEndTime())
                .entryTime(result.getEntryTime())
                .durationMinutes(result.getDurationMinutes())
                .cashierVerifiedAt(result.getCashierVerifiedAt())
                .cashierVerifiedBy(result.getCashierVerifiedBy())
                .checkedInAt(result.getCheckedInAt())
                .checkedInBy(result.getCheckedInBy())
                .participantCount(result.getParticipantCount())
                .skateSizes(result.getSkateSizes())
                .participants(result.getParticipants())
                .onIce(onIce)
                .endsAt(result.getEndsAt())
                .minutesRemaining(result.getMinutesRemaining())
                .minutesUntilSessionEnd(result.getMinutesUntilSessionEnd())
                .ticketType(result.getTicketType())
                .price(result.getPrice())
                .paymentMethod(result.getPaymentMethod())
                .purchaseDate(result.getPurchaseDate())
                .soldByUsername(result.getSoldByUsername())
                .saleReference(result.getSaleReference())
                .build();
    }

    private Payment findPaymentForReservations(List<Reservation> reservations) {
        if (reservations.isEmpty()) {
            return null;
        }
        Set<String> ids = reservations.stream()
                .map(Reservation::getId)
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return null;
        }
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getReservationIds() != null
                        && !payment.getReservationIds().isBlank())
                .filter(payment -> Arrays.stream(payment.getReservationIds().split(","))
                        .map(String::trim)
                        .anyMatch(ids::contains))
                .max(Comparator.comparing(Payment::getPaymentDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private LocalDateTime sessionStart(Session session) {
        if (session == null || session.getSessionDate() == null
                || session.getStartTime() == null) {
            return null;
        }
        return LocalDateTime.of(session.getSessionDate(), session.getStartTime());
    }

    private LocalDateTime sessionEnd(Session session) {
        if (session == null || session.getSessionDate() == null
                || session.getEndTime() == null) {
            return null;
        }
        return LocalDateTime.of(session.getSessionDate(), session.getEndTime());
    }

    private int resolveDuration(Integer durationMinutes, Session session) {
        if (durationMinutes != null && durationMinutes > 0) {
            return durationMinutes;
        }
        if (session != null && session.getStartTime() != null
                && session.getEndTime() != null) {
            long total = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            if (total > 0) {
                return (int) Math.min(DEFAULT_DURATION_MINUTES, total);
            }
        }
        return DEFAULT_DURATION_MINUTES;
    }

    private LocalDateTime resolveEndsAt(Session session, LocalTime entryTime,
                                         Integer durationMinutes,
                                         LocalDateTime checkedInAt) {
        int duration = resolveDuration(durationMinutes, session);
        if (checkedInAt != null) {
            return checkedInAt.plusMinutes(duration);
        }
        LocalDateTime sessionStart = sessionStart(session);
        LocalDateTime sessionEnd = sessionEnd(session);
        LocalDateTime start = null;
        if (session != null && session.getSessionDate() != null && entryTime != null) {
            start = LocalDateTime.of(session.getSessionDate(), entryTime);
        } else {
            start = sessionStart;
        }
        if (start == null) {
            return sessionEnd;
        }
        if (sessionStart != null && start.isBefore(sessionStart)) {
            start = sessionStart;
        }
        LocalDateTime planned = start.plusMinutes(duration);
        return sessionEnd != null && planned.isAfter(sessionEnd) ? sessionEnd : planned;
    }

    private boolean isOnIce(Session session, LocalDateTime checkedInAt, LocalDateTime endsAt) {
        if (checkedInAt == null || endsAt == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(checkedInAt) && endsAt.isAfter(now);
    }

    private long remainingMinutes(LocalDateTime end) {
        if (end == null) {
            return 0;
        }
        long seconds = Duration.between(LocalDateTime.now(), end).getSeconds();
        return seconds <= 0 ? 0 : (seconds + 59) / 60;
    }

    private Optional<Ticket> findTicket(String code, boolean lock) {
        Optional<Ticket> byCode = lock
                ? ticketRepository.findByTicketCodeIgnoreCaseForUpdate(code)
                : ticketRepository.findByTicketCodeIgnoreCase(code);
        if (byCode.isPresent()) {
            return byCode;
        }
        return lock
                ? ticketRepository.findByQrCodeIgnoreCaseForUpdate(code)
                : ticketRepository.findByQrCodeIgnoreCase(code);
    }

    private VerificationResultDto invalid(String code, String message) {
        return baseBuilder(code)
                .valid(false)
                .status("NOT_FOUND")
                .message(message)
                .build();
    }

    private VerificationResultDto.VerificationResultDtoBuilder baseBuilder(String code) {
        return VerificationResultDto.builder()
                .code(code == null ? null : code.trim())
                .valid(false)
                .participantCount(0);
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }

    private String normalize(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
