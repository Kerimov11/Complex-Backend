package az.arena.icearena.dto.admin;

import java.math.BigDecimal;
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
public class ActiveGuestDto {
    private String source;
    private String code;
    private String ticketCode;
    private String ownerName;
    private String ownerUsername;
    private String customerName;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private LocalTime entryTime;
    private Integer durationMinutes;
    private LocalDateTime cashierVerifiedAt;
    private String cashierVerifiedBy;
    private LocalDateTime checkedInAt;
    private String checkedInBy;
    private int participantCount;
    private List<String> skateSizes;
    private List<VerificationParticipantDto> participants;
    private boolean onIce;
    private LocalDateTime endsAt;
    private long minutesRemaining;
    private long minutesUntilSessionEnd;
    private String ticketType;
    private BigDecimal price;
    private String paymentMethod;
    private LocalDateTime purchaseDate;
    private String soldByUsername;
    private String saleReference;
}
