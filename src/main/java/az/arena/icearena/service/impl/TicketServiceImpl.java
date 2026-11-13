// File: src/main/java/az/arena/icearena/service/impl/TicketServiceImpl.java
package az.arena.icearena.service.impl;

import az.arena.icearena.dto.TicketDto;
import az.arena.icearena.dto.TicketPurchaseRequest;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Skate;
import az.arena.icearena.entity.Ticket;
import az.arena.icearena.entity.TicketType;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.mapper.TicketMapper;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.PaymentService;
import az.arena.icearena.service.ReservationActivationService;
import az.arena.icearena.service.TicketService;
import az.arena.icearena.service.CustomerVerificationService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final TicketMapper ticketMapper;
    private final PaymentService paymentService;
    private final CustomerVerificationService customerVerificationService;

    private static final BigDecimal BIG_TICKET_PRICE = new BigDecimal("25.00");
    private static final BigDecimal CHILD_TICKET_PRICE = new BigDecimal("15.00");

    @Override
    @Transactional
    public List<TicketDto> purchaseTickets(TicketPurchaseRequest request, String username) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + request.getSessionId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        customerVerificationService.requireTicketPurchaseEligibility(user);

        if (session.getAvailableTickets() != null) {
            session.setAvailableTickets(Math.max(0, session.getAvailableTickets() - request.getQuantity()));
            sessionRepository.save(session);
        }

        BigDecimal price = request.getTicketType() == TicketType.BIG ? BIG_TICKET_PRICE : CHILD_TICKET_PRICE;
        List<TicketDto> ticketDtos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalTime entryTime = resolveEntryTime(session, now);
        int durationMinutes = 60;

        for (int i = 0; i < request.getQuantity(); i++) {
            String ticketCode = UUID.randomUUID().toString();
            String qrCode = "QR-" + UUID.randomUUID().toString();

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .qrCode(qrCode)
                    .ticketType(request.getTicketType())
                    .price(price)
                    .purchaseDate(now)
                    .isUsed(false)
                    .durationMinutes(durationMinutes)
                    .entryTime(entryTime)
                    .customerName(fullName(user))
                    .user(user)
                    .session(session)
                    .build();

            Ticket savedTicket = ticketRepository.save(ticket);
            paymentService.createPaymentForTicket(savedTicket.getId(), price);
            ticketDtos.add(ticketMapper.toDto(savedTicket));
        }

        return ticketDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDto> getUserTickets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<TicketDto> result = new ArrayList<>();
        result.addAll(ticketRepository.findByUserId(user.getId()).stream()
                .map(ticketMapper::toDto)
                .toList());
        result.addAll(reservationGroups(user.getId()));

        Comparator<TicketDto> newestFirst = Comparator.comparing(
                TicketDto::getPurchaseDate,
                Comparator.nullsLast(Comparator.reverseOrder()));
        result.sort(newestFirst);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDto getMyTicketByCode(String username, String ticketCode) {
        String normalized = ticketCode == null ? "" : ticketCode.trim();
        return getUserTickets(username).stream()
                .filter(ticket -> normalized.equalsIgnoreCase(ticket.getTicketCode())
                        || normalized.equalsIgnoreCase(ticket.getQrCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Bilet tapılmadı: " + ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDto> getTicketsBySession(Long sessionId) {
        return ticketRepository.findBySessionId(sessionId).stream()
                .map(ticketMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDto getTicketByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));
        return ticketMapper.toDto(ticket);
    }

    @Override
    @Transactional
    public void validateTicket(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));
        if (ticket.getIsUsed()) {
            throw new BadRequestException("Ticket has already been used");
        }
        ticket.setIsUsed(true);
        ticketRepository.save(ticket);
    }

    private List<TicketDto> reservationGroups(Long userId) {
        Map<String, List<Reservation>> groups = new LinkedHashMap<>();
        for (Reservation reservation : reservationRepository.findByUserId(userId)) {
            // Ödənişi tamamlanmamış (PENDING) sifarişlər "Biletlərim" bölməsinə düşməməlidir.
            if (reservation.getStatus() == Reservation.Status.PENDING) {
                continue;
            }
            String code = reservation.getVoucherCode();
            if (code == null || code.isBlank()) {
                code = "RES-" + reservation.getId();
            }
            groups.computeIfAbsent(code, key -> new ArrayList<>()).add(reservation);
        }
        return groups.entrySet().stream()
                .map(entry -> reservationGroupToDto(entry.getKey(), entry.getValue()))
                .filter(dto -> !"PENDING".equals(dto.getStatus()))
                .toList();
    }

    private TicketDto reservationGroupToDto(String ticketCode, List<Reservation> reservations) {
        reservations.sort(Comparator.comparing(Reservation::getId,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Reservation first = reservations.get(0);
        Session session = first.getSession();
        LocalDateTime now = LocalDateTime.now();
        boolean past = session != null
                && session.getSessionDate() != null
                && session.getEndTime() != null
                && LocalDateTime.of(session.getSessionDate(), session.getEndTime()).isBefore(now);

        boolean allCancelled = reservations.stream()
                .allMatch(r -> r.getStatus() == Reservation.Status.CANCELLED);
        boolean anyPending = reservations.stream()
                .anyMatch(r -> r.getStatus() == Reservation.Status.PENDING);
        boolean allCheckedIn = reservations.stream().allMatch(r -> r.getCheckedInAt() != null);
        boolean allConfirmed = reservations.stream()
                .allMatch(r -> r.getStatus() == Reservation.Status.CONFIRMED);
        boolean anyCheckedIn = reservations.stream().anyMatch(r -> r.getCheckedInAt() != null);
        boolean anyHandedOut = reservations.stream().anyMatch(r -> r.getSkateHandedOutAt() != null);

        List<Reservation> active = reservations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSkateStockDeducted()))
                .filter(r -> r.getSkateHandedOutAt() == null)
                .filter(r -> r.getTimerExpiresAt() != null && r.getTimerExpiresAt().isAfter(now))
                .toList();
        boolean activeTimer = !active.isEmpty();
        LocalDateTime timerExpiresAt = active.stream()
                .map(Reservation::getTimerExpiresAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        int activationCount = reservations.stream()
                .map(r -> r.getReservationExtensionCount() == null ? 0 : r.getReservationExtensionCount())
                .max(Integer::compareTo)
                .orElse(0);
        int remainingActivations = Math.max(0,
                ReservationActivationServiceImpl.MAX_ACTIVATION_ATTEMPTS - activationCount);

        String status;
        String statusLabel;
        if (allCancelled) {
            status = "CANCELLED";
            statusLabel = "Ləğv edilib";
        } else if (anyPending) {
            status = "PENDING";
            statusLabel = "Ödəniş gözləyir";
        } else if (allCheckedIn) {
            status = "USED";
            statusLabel = "İstifadə edilib";
        } else if (activeTimer) {
            status = "ACTIVE";
            statusLabel = "Konki rezervasiyası aktivdir";
        } else {
            status = "ACTIVE";
            statusLabel = "Aktiv";
        }

        String userReviewStatus = first.getUser() != null ? first.getUser().getIdentityReviewStatus() : null;
        String ticketReviewStatus = first.getReviewStatus();
        String effectiveReviewStatus;
        String effectiveRejectionReason = null;
        if ("REJECTED".equals(ticketReviewStatus)) {
            effectiveReviewStatus = "REJECTED";
            effectiveRejectionReason = first.getReviewReason();
        } else if ("REJECTED".equals(userReviewStatus)) {
            effectiveReviewStatus = "REJECTED";
            effectiveRejectionReason = first.getUser().getIdentityRejectionReason();
        } else if ("APPROVED".equals(ticketReviewStatus) || "APPROVED".equals(userReviewStatus)) {
            effectiveReviewStatus = "APPROVED";
        } else if ("PENDING_REVIEW".equals(ticketReviewStatus) || "PENDING_REVIEW".equals(userReviewStatus)) {
            effectiveReviewStatus = "PENDING_REVIEW";
        } else {
            effectiveReviewStatus = null;
        }
        boolean identityBlocked = "PENDING_REVIEW".equals(effectiveReviewStatus)
                || "REJECTED".equals(effectiveReviewStatus);

        boolean canActivate = allConfirmed
                && !anyCheckedIn
                && !anyHandedOut
                && !activeTimer
                && !identityBlocked
                && remainingActivations > 0;

        List<String> skateSizes = reservations.stream()
                .map(Reservation::getSkate)
                .filter(skate -> skate != null && skate.getSize() != null)
                .sorted(java.util.Comparator.comparing(Skate::getSize))
                .map(Skate::getDisplaySize)
                .toList();
        List<Long> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();
        LocalDateTime purchaseDate = reservations.stream()
                .map(Reservation::getCreatedAt)
                .filter(value -> value != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime checkedInAt = reservations.stream()
                .map(Reservation::getCheckedInAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        int durationMins = first.getDurationMinutes() != null ? first.getDurationMinutes() : 60;
        LocalDateTime checkedOutAt = checkedInAt != null ? checkedInAt.plusMinutes(durationMins) : null;
        String iceEntryTime = null;
        String iceExitTime = null;
        if (checkedInAt != null) {
            ZoneId serverZone = ZoneId.systemDefault();
            ZoneId bakuZone = ZoneId.of("Asia/Baku");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime bakuIn = checkedInAt.atZone(serverZone).withZoneSameInstant(bakuZone).toLocalDateTime();
            LocalDateTime bakuOut = bakuIn.plusMinutes(durationMins);
            iceEntryTime = bakuIn.toLocalTime().format(fmt);
            iceExitTime = bakuOut.toLocalTime().format(fmt);
        }

        return TicketDto.builder()
                .id(first.getId())
                .ticketCode(ticketCode)
                .qrCode(ticketCode)
                .price(null)
                .purchaseDate(purchaseDate)
                .isUsed(allCheckedIn)
                .sessionId(session != null ? session.getId() : null)
                .username(first.getUser() != null ? first.getUser().getUsername() : null)
                .sessionDate(session != null ? session.getSessionDate() : null)
                .sessionStartTime(session != null ? session.getStartTime() : null)
                .sessionEndTime(session != null ? session.getEndTime() : null)
                .durationMinutes(first.getDurationMinutes())
                .status(status)
                .statusLabel(statusLabel)
                .source("BOOKING")
                .reservationIds(reservationIds)
                .skateSizes(skateSizes)
                .participantCount(reservations.size())
                .activationCount(activationCount)
                .maxActivations(ReservationActivationServiceImpl.MAX_ACTIVATION_ATTEMPTS)
                .remainingActivations(remainingActivations)
                .canActivate(canActivate)
                .reservationActive(activeTimer)
                .timerExpiresAt(timerExpiresAt)
                .timerRemainingSeconds(timerExpiresAt == null
                        ? null
                        : Math.max(0, Duration.between(now, timerExpiresAt).getSeconds()))
                .skateStockDeducted(activeTimer)
                .skateHandedOut(anyHandedOut)
                .checkedInAt(checkedInAt)
                .checkedOutAt(checkedOutAt)
                .iceEntryTime(iceEntryTime)
                .iceExitTime(iceExitTime)
                .identityReviewStatus(effectiveReviewStatus)
                .identityRejectionReason(effectiveRejectionReason)
                .build();
    }

    private LocalTime resolveEntryTime(Session session, LocalDateTime now) {
        if (session == null || session.getStartTime() == null) {
            return now.toLocalTime();
        }
        if (session.getSessionDate() == null || !session.getSessionDate().equals(now.toLocalDate())) {
            return session.getStartTime();
        }
        LocalTime current = now.toLocalTime();
        if (current.isBefore(session.getStartTime())) {
            return session.getStartTime();
        }
        if (session.getEndTime() != null && current.isAfter(session.getEndTime())) {
            return session.getEndTime();
        }
        return current;
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }
}
