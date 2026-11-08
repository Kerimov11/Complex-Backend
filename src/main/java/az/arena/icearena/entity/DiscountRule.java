package az.arena.icearena.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin tərəfindən idarə olunan endirim qaydası.
 * Hesablamada istifadə olunan əsas sahələr: faiz, minimum iştirakçı,
 * pulsuz bilet sayı və tələb olunan bilet müddəti.
 */
@Entity
@Table(name = "discount_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private DiscountType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(precision = 5, scale = 2)
    private BigDecimal percent;

    @Column
    private Integer minParticipants;

    @Column
    private Integer freeTickets;

    /** Endirimin tətbiq olunduğu minimum bilet müddəti (dəqiqə). */
    @Column
    private Integer durationMinutes;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
