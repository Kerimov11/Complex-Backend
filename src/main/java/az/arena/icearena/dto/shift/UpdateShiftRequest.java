package az.arena.icearena.dto.shift;

import az.arena.icearena.entity.Station;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Koordinator/admin tərəfindən könüllü smeninin vaxt slotunun və/və ya
 * stansiyasının dəyişdirilməsi üçün sorğu.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateShiftRequest {

    private Long shiftSlotId;

    private Station station;
}
