// File: src/main/java/az/arena/icearena/mapper/SessionMapper.java
package az.arena.icearena.mapper;

import az.arena.icearena.dto.SessionDto;
import az.arena.icearena.entity.Session;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {
    public SessionDto toDto(Session session) {
        if (session == null) {
            return null;
        }
        return SessionDto.builder()
                .id(session.getId())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .maxTicketLimit(session.getMaxTicketLimit())
                .availableTickets(session.getAvailableTickets())
                .blocked(Boolean.TRUE.equals(session.getBlocked()))
                .blockReason(session.getBlockReason())
                .build();
    }

    public Session toEntity(SessionDto sessionDto) {
        if (sessionDto == null) {
            return null;
        }
        return Session.builder()
                .sessionDate(sessionDto.getSessionDate())
                .startTime(sessionDto.getStartTime())
                .endTime(sessionDto.getEndTime())
                .maxTicketLimit(sessionDto.getMaxTicketLimit())
                .availableTickets(sessionDto.getAvailableTickets())
                .blocked(Boolean.TRUE.equals(sessionDto.getBlocked()))
                .blockReason(sessionDto.getBlockReason())
                .build();
    }

    public void updateEntity(Session session, SessionDto sessionDto) {
        if (sessionDto.getSessionDate() != null) {
            session.setSessionDate(sessionDto.getSessionDate());
        }
        if (sessionDto.getStartTime() != null) {
            session.setStartTime(sessionDto.getStartTime());
        }
        if (sessionDto.getEndTime() != null) {
            session.setEndTime(sessionDto.getEndTime());
        }
        if (sessionDto.getMaxTicketLimit() != null) {
            session.setMaxTicketLimit(sessionDto.getMaxTicketLimit());
        }
        if (sessionDto.getAvailableTickets() != null) {
            session.setAvailableTickets(sessionDto.getAvailableTickets());
        }
    }
}
