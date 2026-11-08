package az.arena.icearena.service.impl;

import az.arena.icearena.dto.admin.VerificationResultDto;
import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.Ticket;
import az.arena.icearena.entity.User;
import az.arena.icearena.repository.ReservationRepository;
import az.arena.icearena.repository.TicketRepository;
import az.arena.icearena.service.VerificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public VerificationResultDto verify(String code, String username) {
        String normalized = normalize(code);
        if (normalized == null) {
            return invalid(code, "Kod boşdur.");
        }

        List<Reservation> reservations = reservationRepository
                .findByVoucherCodeIgnoreCase(normalized);
        if (!reservations.isEmpty()) {
            VerificationResultDto check = validateReservations(reservations, normalized);
            if (!check.isValid()) {
                return check;
            }
            LocalDateTime now = LocalDateTime.now();
            for (Reservation reservation : reservations) {
                reservation.setCheckedInAt(now);
                reservation.setCheckedInBy(username);
                reservationRepository.save(reservation);
            }
            check.setCheckedInAt(now);
            check.setCheckedInBy(username);
            check.setStatus("CHECKED_IN");
            check.setMessage("Bilet uğurla təsdiqləndi və giriş qeydə alındı.");
            return check;
        }

        Optional<Ticket> ticket = findTicket(normalized);
        if (ticket.isPresent()) {
            return verifyTicket(ticket.get(), normalized);
        }

        return invalid(code, "Bu koda uyğun bilet tapılmadı.");
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResultDto lookup(String code) {
        String normalized = normalize(code);
        if (normalized == null) {
            return invalid(code, "Kod boşdur.");
        }
        List<Reservation> reservations = reservationRepository
                .findByVoucherCodeIgnoreCase(normalized);
        if (!reservations.isEmpty()) {
            return validateReservations(reservations, normalized);
        }
        Optional<Ticket> ticket = findTicket(normalized);
        if (ticket.isPresent()) {
            return lookupTicket(ticket.get(), normalized);
        }
        return invalid(code, "Bu koda uyğun bilet tapılmadı.");
    }

    private VerificationResultDto validateReservations(List<Reservation> reservations, String code) {
        Reservation first = reservations.get(0);
        Session session = first.getSession();
        User user = first.getUser();
        List<String> sizes = reservations.stream()
                .map(Reservation::getSkate)
                .filter(skate -> skate != null)
                .map(skate -> String.valueOf(skate.getSize()))
                .distinct()
                .toList();
        VerificationResultDto.VerificationResultDtoBuilder builder = baseBuilder(code)
                .source("RESERVATION")
                .ownerName(fullName(user))
                .ownerUsername(user == null ? null : user.getUsername())
                .sessionDate(session == null ? null : session.getSessionDate())
                .sessionStartTime(session == null ? null : session.getStartTime())
                .sessionEndTime(session == null ? null : session.getEndTime())
                .participantCount(reservations.size())
                .skateSizes(sizes);

        if (session != null && session.getSessionDate() != null
                && session.getSessionDate().isBefore(LocalDate.now())) {
            return builder.valid(false).status("EXPIRED")
                    .message("Bu sessiyanın tarixi keçib; bilet etibarsızdır.").build();
        }
        if (reservations.stream().allMatch(r -> r.getCheckedInAt() != null)) {
            Reservation checked = reservations.stream()
                    .filter(r -> r.getCheckedInAt() != null)
                    .findFirst()
                    .orElse(first);
            return builder.valid(false).status("ALREADY_USED")
                    .message("Bu bilet artıq scan edilib.")
                    .checkedInAt(checked.getCheckedInAt())
                    .checkedInBy(checked.getCheckedInBy())
                    .build();
        }
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
                .message("Bilet etibarlıdır; turniketdən keçirilə bilər.").build();
    }

    private VerificationResultDto verifyTicket(Ticket ticket, String code) {
        VerificationResultDto check = lookupTicket(ticket, code);
        if (!check.isValid()) {
            return check;
        }
        ticket.setIsUsed(true);
        ticketRepository.save(ticket);
        check.setStatus("CHECKED_IN");
        check.setMessage("Konki/bilet kodu uğurla təsdiqləndi və istifadə olundu.");
        return check;
    }

    private VerificationResultDto lookupTicket(Ticket ticket, String code) {
        Session session = ticket.getSession();
        User user = ticket.getUser();
        VerificationResultDto.VerificationResultDtoBuilder builder = baseBuilder(code)
                .source("TICKET")
                .ownerName(fullName(user))
                .ownerUsername(user == null ? null : user.getUsername())
                .sessionDate(session == null ? null : session.getSessionDate())
                .sessionStartTime(session == null ? null : session.getStartTime())
                .sessionEndTime(session == null ? null : session.getEndTime())
                .participantCount(1)
                .ticketCode(ticket.getTicketCode());

        if (session != null && session.getSessionDate() != null
                && session.getSessionDate().isBefore(LocalDate.now())) {
            return builder.valid(false).status("EXPIRED")
                    .message("Bu biletin sessiya tarixi keçib.").build();
        }
        if (Boolean.TRUE.equals(ticket.getIsUsed())) {
            return builder.valid(false).status("ALREADY_USED")
                    .message("Bu bilet artıq istifadə edilib.").build();
        }
        return builder.valid(true).status("VALID")
                .message("Bilet etibarlıdır; turniketdən keçirilə bilər.").build();
    }

    private Optional<Ticket> findTicket(String code) {
        Optional<Ticket> byCode = ticketRepository.findByTicketCodeIgnoreCase(code);
        return byCode.isPresent() ? byCode : ticketRepository.findByQrCodeIgnoreCase(code);
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
