package az.arena.icearena.service;

import az.arena.icearena.dto.SessionAvailabilityDto;
import az.arena.icearena.dto.SessionDto;
import az.arena.icearena.entity.Session;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Sessiyanın bağlanış vaxtına görə dinamik bilet müddəti qaydaları.
 *
 * Qaydalar:
 *  - bağlanışa 30 dəqiqədən az qalıbsa satış bağlanır;
 *  - 60 dəqiqəyə yer qalmayıbsa yalnız 30 dəqiqəlik bilet satılır;
 *  - keçmiş tarix / sessiya xaricində satış yoxdur.
 */
@Service
public class SessionTimeService {

    private static final int MIN_TICKET_MINUTES = 30;
    private static final int HOUR_TICKET_MINUTES = 60;

    public SessionAvailabilityDto evaluate(Session session,
                                           LocalTime requestedStart,
                                           Integer requestedDuration) {
        return evaluate(session, requestedStart, requestedDuration, LocalDateTime.now());
    }

    public SessionAvailabilityDto evaluate(Session session,
                                           LocalTime requestedStart,
                                           Integer requestedDuration,
                                           LocalDateTime now) {
        LocalDate date = session.getSessionDate();
        LocalTime sessionStart = session.getStartTime();
        LocalTime sessionEnd = session.getEndTime();
        int totalMinutes = minutesBetween(sessionStart, sessionEnd);
        LocalTime lastStart = sessionEnd == null ? null : sessionEnd.minusMinutes(MIN_TICKET_MINUTES);

        LocalTime effectiveStart = requestedStart;
        if (effectiveStart == null) {
            if (date != null && date.equals(now.toLocalDate())) {
                effectiveStart = now.toLocalTime().isBefore(sessionStart)
                        ? sessionStart
                        : (now.toLocalTime().isAfter(sessionEnd) ? sessionEnd : now.toLocalTime());
            } else {
                effectiveStart = sessionStart;
            }
        }

        int remaining = Math.max(0, minutesBetween(effectiveStart, sessionEnd));
        boolean datePast = date != null && date.isBefore(now.toLocalDate());
        boolean outside = effectiveStart.isBefore(sessionStart) || !effectiveStart.isBefore(sessionEnd);
        boolean pastTime = date != null
                && date.equals(now.toLocalDate())
                && effectiveStart.isBefore(now.toLocalTime().minusMinutes(5));
        boolean blocked = Boolean.TRUE.equals(session.getBlocked());

        boolean salesOpen = !blocked && !datePast && !outside && !pastTime
                && remaining >= MIN_TICKET_MINUTES;
        List<Integer> durations = new ArrayList<>();
        if (salesOpen) {
            durations.add(MIN_TICKET_MINUTES);
            if (remaining >= HOUR_TICKET_MINUTES) {
                durations.add(HOUR_TICKET_MINUTES);
            }
        }

        boolean requestedAvailable = requestedDuration == null
                || durations.contains(requestedDuration);

        String reason;
        String message;
        if (blocked) {
            reason = "SESSION_BLOCKED";
            message = session.getBlockReason() == null || session.getBlockReason().isBlank()
                    ? "Bu sessiya müvəqqəti olaraq bloklanıb."
                    : "Sessiya bloklanıb: " + session.getBlockReason();
        } else if (datePast) {
            reason = "PAST_DATE";
            message = "Keçmiş tarix üçün bilet satışı mümkün deyil.";
        } else if (outside) {
            reason = "OUTSIDE_SESSION";
            message = "Seçilmiş başlama vaxtı sessiyanın iş aralığına düşmür.";
        } else if (pastTime) {
            reason = "PAST_TIME";
            message = "Seçilmiş başlama vaxtı artıq keçib.";
        } else if (remaining < MIN_TICKET_MINUTES) {
            reason = "LESS_THAN_30_MINUTES";
            message = "Sessiyanın bağlanmasına 30 dəqiqədən az qalıb; bilet satışı dayandırılıb.";
        } else if (!requestedAvailable) {
            reason = "DURATION_NOT_AVAILABLE";
            message = "Bu sessiyada seçilmiş müddət üçün kifayət qədər vaxt yoxdur.";
        } else {
            reason = "AVAILABLE";
            message = remaining >= HOUR_TICKET_MINUTES
                    ? "1 saat və 30 dəqiqəlik bilet mövcuddur."
                    : "Yalnız 30 dəqiqəlik bilet mövcuddur.";
        }

        return SessionAvailabilityDto.builder()
                .sessionId(session.getId())
                .sessionDate(date)
                .sessionStartTime(sessionStart)
                .sessionEndTime(sessionEnd)
                .requestedStartTime(requestedStart)
                .lastStartTime(lastStart)
                .totalMinutes(totalMinutes)
                .remainingMinutes(remaining)
                .salesOpen(salesOpen)
                .requestedDurationAvailable(requestedAvailable)
                .availableDurations(durations)
                .reason(reason)
                .message(message)
                .build();
    }

    public void enrich(SessionDto dto, Session session, LocalDateTime now) {
        if (dto == null || session == null) {
            return;
        }
        SessionAvailabilityDto availability = evaluate(session, null, null, now);
        dto.setTotalMinutes(availability.getTotalMinutes());
        dto.setRemainingMinutes(availability.getRemainingMinutes());
        dto.setLastStartTime(availability.getLastStartTime());
        dto.setSalesOpen(availability.getSalesOpen());
        dto.setAvailableDurations(availability.getAvailableDurations());
        int max = session.getMaxTicketLimit() == null ? 0 : session.getMaxTicketLimit();
        int available = session.getAvailableTickets() == null ? 0 : session.getAvailableTickets();
        int occupied = Math.max(0, max - available);
        dto.setOccupiedTickets(occupied);
        dto.setOccupancyRate(max == 0 ? 0d : Math.round((occupied * 10000d) / max) / 100d);
    }

    private int minutesBetween(LocalTime from, LocalTime to) {
        if (from == null || to == null) {
            return 0;
        }
        long minutes = Duration.between(from, to).toMinutes();
        return (int) Math.max(0, minutes);
    }
}
