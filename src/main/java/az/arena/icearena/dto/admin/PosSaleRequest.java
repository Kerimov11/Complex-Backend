package az.arena.icearena.dto.admin;

import az.arena.icearena.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSaleRequest {
    @NotNull(message = "Sessiya seçilməlidir")
    private Long sessionId;

    @NotNull(message = "Bilet sayı tələb olunur")
    @Min(value = 1, message = "Ən azı 1 bilet satılmalıdır")
    private Integer quantity;

    private Integer durationMinutes;

    @NotNull(message = "Ödəniş üsulu seçilməlidir")
    private PaymentMethod paymentMethod;

    private String customerName;
    private String customerPhone;
}
