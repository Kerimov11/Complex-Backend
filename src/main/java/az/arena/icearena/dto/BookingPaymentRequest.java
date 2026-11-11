package az.arena.icearena.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPaymentRequest {

    @NotNull(message = "Məbləğ tələb olunur")
    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal amount;

    @Size(max = 100, message = "Referans maksimum 100 simvol ola bilər")
    private String reference;

    /** Frontend-in yaratdığı rezervasiyalar. Ödəniş uğursuz olarsa backend onları ləğv edir. */
    @Size(max = 1000, message = "Ən çoxu 1000 rezervasiya ola bilər")
    private List<Long> reservationIds;

    /** Qiymətin serverdə yenidən yoxlanması üçün əlavə məlumat. */
    private LocalDate date;
    private LocalTime startTime;
    private Integer durationMinutes;

    private Boolean student;
    private Boolean family;
    private Boolean push30;
    private Boolean qazi;
    private Integer qaziVisits;
    private Boolean shaheed;
    private Boolean shaheedVerified;
}
