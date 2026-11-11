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
public class VerificationResultDto {
    private boolean valid;
    private String status;
    private String message;
    private String code;
    private String source;
    private String ownerName;
    private String ownerUsername;
    private String customerName;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private LocalTime entryTime;
    private Integer durationMinutes;
    private int participantCount;
    private List<String> skateSizes;
    private List<VerificationParticipantDto> participants;
    private List<Long> reservationIds;
    private LocalDateTime cashierVerifiedAt;
    private String cashierVerifiedBy;
    private LocalDateTime checkedInAt;
    private String checkedInBy;
    private String ticketCode;
    private String ticketType;
    private BigDecimal price;
    private String paymentMethod;
    private LocalDateTime purchaseDate;
    private String soldByUsername;
    private String saleReference;
    private boolean activeOnIce;
    private LocalDateTime endsAt;
    private long minutesRemaining;
    private long minutesUntilSessionEnd;
    private boolean skateHandedOut;
    private LocalDateTime skateHandedOutAt;
    private String skateHandedOutBy;
    private boolean skateStockDeducted;
    private boolean canHandover;
}
