package az.arena.icearena.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class IdentityVerificationAdminDto {

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String identityDocumentNumber;
    private String finCode;
    private String identityPhotoData;
    private String identityReviewStatus;
    private LocalDateTime identityConfirmedAt;
    private LocalDateTime identityReviewedAt;
    private String identityReviewedBy;
    private String identityRejectionReason;

    @Builder.Default
    private List<TicketSummary> tickets = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketSummary {
        private String ticketCode;
        private LocalDate sessionDate;
        private Integer durationMinutes;
        private Integer participantCount;
        @Builder.Default
        private List<String> skateSizes = new ArrayList<>();
        private String status;
        private LocalDateTime purchaseDate;
    }
}
