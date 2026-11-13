// File: src/main/java/az/arena/icearena/dto/TicketDto.java
package az.arena.icearena.dto;

import az.arena.icearena.entity.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class TicketDto {
    private Long id;
    private String ticketCode;
    private String qrCode;
    private TicketType ticketType;
    private BigDecimal price;
    private LocalDateTime purchaseDate;
    private Boolean isUsed;
    private Long sessionId;
    private String username;

    // Bilet tarixçəsi / sessiya məlumatı
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private Integer durationMinutes;
    private String status;
    private String statusLabel;
    private String source;

    // Rezervasiya qrupu (onlayn bilet) məlumatı
    @Builder.Default
    private List<Long> reservationIds = new ArrayList<>();
    @Builder.Default
    private List<String> skateSizes = new ArrayList<>();
    private Integer participantCount;
    private Integer activationCount;
    private Integer maxActivations;
    private Integer remainingActivations;
    private Boolean canActivate;
    private Boolean reservationActive;
    private LocalDateTime timerExpiresAt;
    private Long timerRemainingSeconds;
    private Boolean skateStockDeducted;
    private Boolean skateHandedOut;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private String iceEntryTime;
    private String iceExitTime;
}
