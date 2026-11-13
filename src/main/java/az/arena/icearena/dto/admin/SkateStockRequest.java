package az.arena.icearena.dto.admin;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkateStockRequest {
    private Integer size;

    @Min(value = 0, message = "Say mənfi ola bilməz")
    private Integer quantity;

    @Min(value = 0, message = "Ümumi say mənfi ola bilməz")
    private Integer totalQuantity;

    @Min(value = 0, message = "Mövcud say mənfi ola bilməz")
    private Integer availableQuantity;

    @Min(value = 0, message = "Zədəli say mənfi ola bilməz")
    private Integer damagedQuantity;
}
