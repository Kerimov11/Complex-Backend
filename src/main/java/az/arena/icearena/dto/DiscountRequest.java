package az.arena.icearena.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Səbət / checkout üçün qiymət hesablama sorğusu.
 * Qrup və 5+1 endirimləri burada avtomatik hesablanır; istifadəçi onları seçmir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRequest {

    /** Əgər verilirsə, cavabda sessiya üzrə qalıq vaxt da qaytarılır. */
    private Long sessionId;

    @NotNull(message = "Tarix tələb olunur")
    private LocalDate date;

    @NotNull(message = "Başlama vaxtı tələb olunur")
    private LocalTime startTime;

    @NotNull(message = "Bilet müddəti tələb olunur")
    @Min(value = 30, message = "Müddət ən azı 30 dəqiqə olmalıdır")
    @Max(value = 60, message = "Müddət ən çoxu 60 dəqiqə ola bilər")
    private Integer durationMinutes;

    @NotNull(message = "İştirakçı sayı tələb olunur")
    @Min(value = 1, message = "Ən azı 1 iştirakçı olmalıdır")
    @Max(value = 1000, message = "Ən çoxu 1000 iştirakçı ola bilər")
    private Integer quantity;

    /** Sosial güzəşt hüququ: tələbə. */
    private Boolean student;
    /** Qrup sifarişində tələbə olan iştirakçı sayı (null olduqda hamısı). */
    private Integer studentCount;
    /** Sosial güzəşt hüququ: ailə. */
    private Boolean family;
    /** PUSH30 premium paketi. */
    private Boolean push30;
    /** Qazi statusu. */
    private Boolean qazi;
    /** Qazinin əvvəlki giriş sayı (0 = ilk giriş). */
    private Integer qaziVisits;
    /** Şəhid ailəsi statusu. */
    private Boolean shaheed;
    /** Şəhidlik sənədi təsdiqli olub? */
    private Boolean shaheedVerified;
}
