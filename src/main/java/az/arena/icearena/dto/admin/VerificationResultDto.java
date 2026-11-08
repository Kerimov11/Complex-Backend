package az.arena.icearena.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResultDto {
    private boolean valid;
    private String status;
    private String message;
    private String code;
    private String source;
    private String ownerName;
    private String ownerUsername;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private int participantCount;
    private List<String> skateSizes;
    private LocalDateTime checkedInAt;
    private String checkedInBy;
    private String ticketCode;
}
