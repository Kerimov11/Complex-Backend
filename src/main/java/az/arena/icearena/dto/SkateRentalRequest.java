// File: src/main/java/az/arena/icearena/dto/SkateRentalRequest.java
package az.arena.icearena.dto;

import jakarta.validation.constraints.Max;
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
public class SkateRentalRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Skate size is required")
    @Min(value = 35, message = "Skate size must be at least 35")
    @Max(value = 45, message = "Skate size must be at most 45")
    private Integer skateSize;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
