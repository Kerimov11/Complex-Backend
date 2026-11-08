package az.arena.icearena.dto.admin;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionOccupancyDto {
    private Long id;
    private String startTime;
    private String endTime;
    private int maxTickets;
    private int availableTickets;
    private int occupiedTickets;
    private double occupancyRate;
    private boolean blocked;
    private boolean active;
    private LocalTime start;
    private LocalTime end;
}
