package az.arena.icearena.dto.customer;

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
public class TicketPhotosRequest {

    private String voucherCode;
    private Integer studentCount;
    private String familyIdsPhotoData;
    private String push30ScreenshotData;
    private String push30Phone;

    @Builder.Default
    private List<ParticipantPhoto> participants = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantPhoto {
        private Long reservationId;
        private String participantName;
        private String identityPhotoData;
        private String studentCardPhotoData;
        private Boolean studentDiscountApplied;
    }
}
