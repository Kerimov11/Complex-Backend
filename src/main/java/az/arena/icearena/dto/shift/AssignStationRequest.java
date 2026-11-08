package az.arena.icearena.dto.shift;

import az.arena.icearena.entity.Station;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignStationRequest {

    @NotNull(message = "Volunteer smenası tələb olunur")
    private Long volunteerShiftId;

    @NotNull(message = "Stansiya tələb olunur")
    private Station station;
}
