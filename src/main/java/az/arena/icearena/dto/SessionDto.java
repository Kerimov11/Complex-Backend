package az.arena.icearena.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDto {
    private Long id;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxTicketLimit;
    private Integer availableTickets;
    private Boolean blocked;
    private String blockReason;
    private Integer occupiedTickets;
    private Double occupancyRate;

    // Dinamik bağlanış / bilet müddəti məlumatı
    private Integer totalMinutes;
    private Integer remainingMinutes;
    private LocalTime lastStartTime;
    private Boolean salesOpen;
    @Builder.Default
    private List<Integer> availableDurations = new ArrayList<>();
}
