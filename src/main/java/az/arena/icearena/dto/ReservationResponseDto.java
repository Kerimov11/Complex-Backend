package az.arena.icearena.dto;

import az.arena.icearena.entity.Reservation;
import az.arena.icearena.entity.Skate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ReservationController cavabı üçün təhlükəsiz DTO.
 * Həssas sənəd/foto sahələrini (identityPhotoData, studentCardPhotoData və s.)
 * API cavabına daxil etmir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponseDto {

    private Long id;
    private SkateInfo skate;
    private Reservation.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime timerExpiresAt;
    private LocalTime entryTime;
    private Integer durationMinutes;
    private Integer reservationExtensionCount;
    private Integer extensionCount;
    private Boolean skateStockDeducted;
    private LocalDateTime skateReservedAt;
    private String voucherCode;
    private String participantName;
    private Integer participantAge;
    private Boolean studentDiscountApplied;
    private String discountType;
    private String reviewStatus;
    private String reviewReason;
    private LocalDateTime cashierVerifiedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime skateHandedOutAt;
    private LocalDateTime skateReturnedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkateInfo {
        private Long id;
        private Integer size;
        private String label;
        private String displaySize;
    }

    public static ReservationResponseDto fromEntity(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        Skate skate = reservation.getSkate();
        SkateInfo skateInfo = skate == null ? null : SkateInfo.builder()
                .id(skate.getId())
                .size(skate.getSize())
                .label(skate.getLabel())
                .displaySize(skate.getDisplaySize())
                .build();

        return ReservationResponseDto.builder()
                .id(reservation.getId())
                .skate(skateInfo)
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .timerExpiresAt(reservation.getTimerExpiresAt())
                .entryTime(reservation.getEntryTime())
                .durationMinutes(reservation.getDurationMinutes())
                .reservationExtensionCount(reservation.getReservationExtensionCount())
                .extensionCount(reservation.getReservationExtensionCount())
                .skateStockDeducted(reservation.getSkateStockDeducted())
                .skateReservedAt(reservation.getSkateReservedAt())
                .voucherCode(reservation.getVoucherCode())
                .participantName(reservation.getParticipantName())
                .participantAge(reservation.getParticipantAge())
                .studentDiscountApplied(reservation.getStudentDiscountApplied())
                .discountType(reservation.getDiscountType())
                .reviewStatus(reservation.getReviewStatus())
                .reviewReason(reservation.getReviewReason())
                .cashierVerifiedAt(reservation.getCashierVerifiedAt())
                .checkedInAt(reservation.getCheckedInAt())
                .skateHandedOutAt(reservation.getSkateHandedOutAt())
                .skateReturnedAt(reservation.getSkateReturnedAt())
                .build();
    }
}
