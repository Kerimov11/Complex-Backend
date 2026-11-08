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
}
