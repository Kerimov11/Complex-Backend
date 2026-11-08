package az.arena.icearena.service;

import az.arena.icearena.dto.shift.VolunteerShiftDto;
import az.arena.icearena.entity.Station;
import java.util.List;

public interface VolunteerShiftService {
    List<VolunteerShiftDto> listMine(String username);

    VolunteerShiftDto selectShift(String username, Long shiftSlotId);

    VolunteerShiftDto assignStation(Long volunteerShiftId, Station station, String actorUsername);

    VolunteerShiftDto updateShift(Long volunteerShiftId, Long shiftSlotId, Station station,
                                  String actorUsername);

    void deleteShift(Long volunteerShiftId, String actorUsername);
}
