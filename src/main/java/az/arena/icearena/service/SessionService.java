// File: src/main/java/az/arena/icearena/service/SessionService.java
package az.arena.icearena.service;

import az.arena.icearena.dto.SessionAvailabilityDto;
import az.arena.icearena.dto.SessionDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SessionService {
    SessionDto createSession(SessionDto sessionDto);

    SessionDto updateSession(Long sessionId, SessionDto sessionDto);

    void deleteSession(Long sessionId);

    SessionDto blockSession(Long sessionId, String reason);

    SessionDto unblockSession(Long sessionId);

    SessionDto getSessionById(Long sessionId);

    List<SessionDto> getAllSessions();

    List<SessionDto> getSessionsByDate(LocalDate sessionDate);

    List<SessionDto> getSessionsByDateRange(LocalDate startDate, LocalDate endDate);

    SessionAvailabilityDto getSessionAvailability(Long sessionId, LocalTime startTime, Integer durationMinutes);
}
