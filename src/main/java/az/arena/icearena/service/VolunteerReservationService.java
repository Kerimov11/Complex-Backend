package az.arena.icearena.service;

import az.arena.icearena.dto.volunteer.VolunteerReservationDto;
import java.util.List;

public interface VolunteerReservationService {
    List<VolunteerReservationDto> listActiveReservations();

    List<VolunteerReservationDto> listLiveReservations();

    VolunteerReservationDto reserve(Long reservationId, String username);

    VolunteerReservationDto fulfill(Long reservationId, String username);
}
