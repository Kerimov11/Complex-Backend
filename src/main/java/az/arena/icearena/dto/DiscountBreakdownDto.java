package az.arena.icearena.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Itemized qiymət cavabı. Frontend bunu birbaşa sifariş yekununda göstərir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountBreakdownDto {

    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;

    /** Məs. GROUP, FIVE_PLUS_ONE, STUDENT, NONE. */
    private String appliedDiscountType;
    private String appliedDiscountLabel;

    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer durationMinutes;
    private Integer freeTickets;
    private String dayType;

    /** Cavabın hesablandığı an üçün sessiya məlumatı. */
    private Integer remainingMinutes;
    private Boolean salesOpen;
    private List<Integer> availableDurations = new ArrayList<>();

    private Boolean groupEligible;
    private Boolean fivePlusOneEligible;

    @Builder.Default
    private List<PriceLineDto> lines = new ArrayList<>();
    @Builder.Default
    private List<String> notes = new ArrayList<>();
}
