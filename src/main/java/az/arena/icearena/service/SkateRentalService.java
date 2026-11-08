// File: src/main/java/az/arena/icearena/service/SkateRentalService.java
package az.arena.icearena.service;

import az.arena.icearena.dto.SkateRentalDto;
import az.arena.icearena.dto.SkateRentalRequest;
import java.util.List;

public interface SkateRentalService {
    SkateRentalDto rentSkates(SkateRentalRequest request, String username);

    List<SkateRentalDto> getUserSkateRentals(String username);

    List<SkateRentalDto> getSkateRentalsBySession(Long sessionId);
}
