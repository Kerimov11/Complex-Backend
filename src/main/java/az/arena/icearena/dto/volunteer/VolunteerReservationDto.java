package az.arena.icearena.dto.volunteer;

import az.arena.icearena.entity.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerReservationDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerUsername;
    private Long sessionId;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private LocalTime entryTime;
    private Integer durationMinutes;
    private Integer skateSize;
    private Reservation.Status status;
    private String statusLabel;
    private boolean skateHandedOut;
    private LocalDateTime skateHandedOutAt;
    private String skateHandedOutBy;
    private String voucherCode;

    private Integer activationCount;
    private Integer remainingActivations;
    private LocalDateTime timerExpiresAt;
    private Long timerRemainingSeconds;
    private boolean reservationActive;
    private boolean skateStockDeducted;
    private boolean skateReserved;
    private LocalDateTime skateReservedAt;
    private String skateReservedBy;
}
