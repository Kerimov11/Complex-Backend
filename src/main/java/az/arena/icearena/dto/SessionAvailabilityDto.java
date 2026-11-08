package az.arena.icearena.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Müəyyən sessiya və başlama vaxtı üçün dinamik mövcudluq cavabı.
 * Bağlanışa 30 dəqiqədən az qalırsa satış bağlanır; 60 dəqiqəyə yer
 * qalmayıbsa yalnız 30 dəqiqəlik bilet təklif olunur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionAvailabilityDto {

    private Long sessionId;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private LocalTime requestedStartTime;
    private LocalTime lastStartTime;

    private Integer totalMinutes;
    private Integer remainingMinutes;
    private Boolean salesOpen;
    private Boolean requestedDurationAvailable;

    @Builder.Default
    private List<Integer> availableDurations = new ArrayList<>();

    private String reason;
    private String message;
}
