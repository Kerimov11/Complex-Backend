// File: src/main/java/az/arena/icearena/dto/SkateRentalDto.java
package az.arena.icearena.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkateRentalDto {
    private Long id;
    private Integer skateSize;
    private Integer quantity;
    private BigDecimal pricePerPair;
    private BigDecimal totalPrice;
    private LocalDateTime rentalDate;
    private Long sessionId;
    private String username;
}
