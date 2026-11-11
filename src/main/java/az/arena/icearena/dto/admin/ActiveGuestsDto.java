package az.arena.icearena.dto.admin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveGuestsDto {
    private List<ActiveGuestDto> activeTickets;
    private List<ActiveGuestDto> onIce;
}
