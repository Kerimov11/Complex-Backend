package az.arena.icearena.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationParticipantDto {
    private Long reservationId;
    private String name;
    private Integer age;
    private String skateSize;
    private boolean skateStockDeducted;
    private boolean skateHandedOut;
    private LocalDateTime skateHandedOutAt;
    private String skateHandedOutBy;
}
