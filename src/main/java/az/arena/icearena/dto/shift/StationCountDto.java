package az.arena.icearena.dto.shift;

import az.arena.icearena.entity.Station;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationCountDto {
    private Station station;
    private String label;
    private long count;
}
