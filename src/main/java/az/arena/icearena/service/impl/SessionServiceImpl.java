// File: src/main/java/az/arena/icearena/service/impl/SessionServiceImpl.java
package az.arena.icearena.service.impl;

import az.arena.icearena.dto.SessionAvailabilityDto;
import az.arena.icearena.dto.SessionDto;
import az.arena.icearena.entity.Session;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.mapper.SessionMapper;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.service.SessionService;
import az.arena.icearena.service.SessionTimeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final SessionTimeService sessionTimeService;

    @Override
    @Transactional
    public SessionDto createSession(SessionDto sessionDto) {
        Session session = sessionMapper.toEntity(sessionDto);
        if (session.getAvailableTickets() == null) {
            session.setAvailableTickets(session.getMaxTicketLimit());
        }
        Session savedSession = sessionRepository.save(session);
        return enrich(sessionMapper.toDto(savedSession), savedSession);
    }

    @Override
    @Transactional
    public SessionDto updateSession(Long sessionId, SessionDto sessionDto) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        Integer previousMax = session.getMaxTicketLimit();
        Integer previousAvailable = session.getAvailableTickets();
        sessionMapper.updateEntity(session, sessionDto);
        if (sessionDto.getAvailableTickets() == null && sessionDto.getMaxTicketLimit() != null) {
            int oldMax = previousMax == null ? 0 : previousMax;
            int newMax = session.getMaxTicketLimit() == null ? 0 : session.getMaxTicketLimit();
            int oldAvailable = previousAvailable == null ? oldMax : previousAvailable;
            int adjusted = oldAvailable + (newMax - oldMax);
            session.setAvailableTickets(Math.max(0, Math.min(newMax, adjusted)));
        }
        Session updatedSession = sessionRepository.save(session);
        return enrich(sessionMapper.toDto(updatedSession), updatedSession);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found with id: " + sessionId);
        }
        sessionRepository.deleteById(sessionId);
    }

    @Override
    @Transactional
    public SessionDto blockSession(Long sessionId, String reason) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        session.setBlocked(true);
        session.setBlockReason(reason == null || reason.isBlank()
                ? "Admin tərəfindən bloklanıb"
                : reason.trim());
        Session saved = sessionRepository.save(session);
        return enrich(sessionMapper.toDto(saved), saved);
    }

    @Override
    @Transactional
    public SessionDto unblockSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        session.setBlocked(false);
        session.setBlockReason(null);
        Session saved = sessionRepository.save(session);
        return enrich(sessionMapper.toDto(saved), saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionDto getSessionById(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        return enrich(sessionMapper.toDto(session), session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> getAllSessions() {
        LocalDateTime now = LocalDateTime.now();
        return sessionRepository.findAll().stream()
                .map(session -> enrich(sessionMapper.toDto(session), session, now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> getSessionsByDate(LocalDate sessionDate) {
        LocalDateTime now = LocalDateTime.now();
        return sessionRepository.findBySessionDate(sessionDate).stream()
                .sorted(Comparator.comparing(Session::getStartTime))
                .map(session -> enrich(sessionMapper.toDto(session), session, now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> getSessionsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        return sessionRepository.findBySessionDateBetween(startDate, endDate).stream()
                .sorted(Comparator.comparing(Session::getSessionDate)
                        .thenComparing(Session::getStartTime))
                .map(session -> enrich(sessionMapper.toDto(session), session, now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionAvailabilityDto getSessionAvailability(Long sessionId,
                                                         LocalTime startTime,
                                                         Integer durationMinutes) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        return sessionTimeService.evaluate(session, startTime, durationMinutes);
    }

    private SessionDto enrich(SessionDto dto, Session session) {
        return enrich(dto, session, LocalDateTime.now());
    }

    private SessionDto enrich(SessionDto dto, Session session, LocalDateTime now) {
        sessionTimeService.enrich(dto, session, now);
        return dto;
    }
}
