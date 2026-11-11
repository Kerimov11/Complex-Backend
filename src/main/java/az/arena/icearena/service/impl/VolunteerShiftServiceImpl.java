package az.arena.icearena.service.impl;

import az.arena.icearena.dto.shift.VolunteerShiftDto;
import az.arena.icearena.entity.RoleName;
import az.arena.icearena.entity.ShiftAssignmentStatus;
import az.arena.icearena.entity.ShiftSlot;
import az.arena.icearena.entity.Station;
import az.arena.icearena.entity.User;
import az.arena.icearena.entity.VolunteerShift;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.mapper.ShiftMapper;
import az.arena.icearena.repository.ShiftSlotRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.repository.VolunteerShiftRepository;
import az.arena.icearena.service.VolunteerShiftService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VolunteerShiftServiceImpl implements VolunteerShiftService {

    private final VolunteerShiftRepository volunteerShiftRepository;
    private final ShiftSlotRepository shiftSlotRepository;
    private final UserRepository userRepository;
    private final ShiftMapper shiftMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerShiftDto> listMine(String username) {
        User user = findUser(username);
        LocalDateTime now = LocalDateTime.now();
        return volunteerShiftRepository
                .findByUserIdOrderBySelectedAtDesc(user.getId())
                .stream()
                .map(shift -> shiftMapper.toDto(shift, now))
                .sorted(Comparator
                        .comparing(VolunteerShiftDto::getShiftDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(VolunteerShiftDto::getStartTime,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    @Transactional
    public VolunteerShiftDto selectShift(String username, Long shiftSlotId) {
        User user = findUser(username);
        ensureVolunteer(user);
        if (shiftSlotId == null) {
            throw new BadRequestException("Smen slotu seçilməlidir.");
        }
        ShiftSlot slot = shiftSlotRepository.findByIdAndActiveTrue(shiftSlotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aktiv smen slotu tapılmadı: " + shiftSlotId));
        LocalDateTime now = LocalDateTime.now();
        if (slot.getShiftDate().isBefore(LocalDate.now())
                || (slot.getShiftDate().equals(LocalDate.now())
                && slot.getEndTime().isBefore(now.toLocalTime()))) {
            throw new BadRequestException("Keçmiş smenə yazılmaq mümkün deyil.");
        }

        VolunteerShift shift = volunteerShiftRepository
                .findByUserIdAndShiftSlotId(user.getId(), slot.getId())
                .orElseGet(() -> volunteerShiftRepository.save(VolunteerShift.builder()
                        .user(user)
                        .shiftSlot(slot)
                        .status(ShiftAssignmentStatus.SELECTED)
                        .selectedAt(now)
                        .build()));
        return shiftMapper.toDto(shift, now);
    }

    @Override
    @Transactional
    public VolunteerShiftDto assignStation(Long volunteerShiftId,
                                           Station station,
                                           String actorUsername) {
        if (station == null) {
            throw new BadRequestException("Stansiya seçilməlidir.");
        }
        VolunteerShift shift = findShift(volunteerShiftId);
        if (shift.getStatus() == ShiftAssignmentStatus.CANCELLED) {
            throw new BadRequestException("Ləğv edilmiş smenə stansiya təyin edilə bilməz.");
        }
        shift.setStation(station);
        shift.setStatus(ShiftAssignmentStatus.ASSIGNED);
        shift.setAssignedAt(LocalDateTime.now());
        shift.setAssignedBy(actorUsername);
        volunteerShiftRepository.save(shift);
        return shiftMapper.toDto(shift, LocalDateTime.now());
    }

    @Override
    @Transactional
    public VolunteerShiftDto updateShift(Long volunteerShiftId,
                                         Long shiftSlotId,
                                         Station station,
                                         String actorUsername) {
        if (shiftSlotId == null && station == null) {
            throw new BadRequestException("Yeni smen vaxtı və ya stansiya seçilməlidir.");
        }
        VolunteerShift shift = findShift(volunteerShiftId);
        if (shift.getStatus() == ShiftAssignmentStatus.CANCELLED) {
            throw new BadRequestException("Ləğv edilmiş smen dəyişdirilə bilməz.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (shiftSlotId != null && !shiftSlotId.equals(shift.getShiftSlot().getId())) {
            ShiftSlot newSlot = shiftSlotRepository.findByIdAndActiveTrue(shiftSlotId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Aktiv smen slotu tapılmadı: " + shiftSlotId));
            if (newSlot.getShiftDate().isBefore(LocalDate.now())
                    || (newSlot.getShiftDate().equals(LocalDate.now())
                    && newSlot.getEndTime().isBefore(now.toLocalTime()))) {
                throw new BadRequestException("Keçmiş smenə keçid etmək mümkün deyil.");
            }
            Long userId = shift.getUser().getId();
            volunteerShiftRepository.findByUserIdAndShiftSlotId(userId, newSlot.getId())
                    .ifPresent(existing -> {
                        throw new BadRequestException(
                                "Könüllünün bu smen vaxtında artıq qeydiyyatı var.");
                    });
            shift.setShiftSlot(newSlot);
            shift.setSelectedAt(now);
        }

        if (station != null) {
            shift.setStation(station);
            shift.setStatus(ShiftAssignmentStatus.ASSIGNED);
        } else if (shift.getStation() != null) {
            shift.setStatus(ShiftAssignmentStatus.ASSIGNED);
        }

        shift.setAssignedAt(now);
        shift.setAssignedBy(actorUsername);
        volunteerShiftRepository.save(shift);
        return shiftMapper.toDto(shift, now);
    }

    @Override
    @Transactional
    public void deleteShift(Long volunteerShiftId, String actorUsername) {
        VolunteerShift shift = findShift(volunteerShiftId);
        volunteerShiftRepository.delete(shift);
    }

    private VolunteerShift findShift(Long volunteerShiftId) {
        return volunteerShiftRepository.findById(volunteerShiftId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Könüllü smeni tapılmadı: " + volunteerShiftId));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "İstifadəçi tapılmadı: " + username));
    }

    private void ensureVolunteer(User user) {
        boolean allowed = user.getRoles().stream()
                .map(role -> role.getName())
                .anyMatch(role -> role == RoleName.ROLE_VOLUNTEER
                        || role == RoleName.ROLE_COORDINATOR);
        if (!allowed) {
            throw new BadRequestException(
                    "Smen seçimi yalnız VOLUNTEER və ya COORDINATOR rolu üçün mümkündür.");
        }
    }
}
