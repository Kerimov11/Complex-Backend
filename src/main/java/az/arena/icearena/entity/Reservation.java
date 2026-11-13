package az.arena.icearena.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne
    @JoinColumn(name = "skate_id", nullable = false)
    private Skate skate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // PENDING, CONFIRMED, CANCELLED, EXPIRED

    private LocalDateTime createdAt;

    private LocalDateTime timerExpiresAt; // Taymerin bitmə vaxtı

    /** Biletin faktiki başlama vaxtı (məs. 13:15). */
    private java.time.LocalTime entryTime;

    /** Bilet müddəti: 30 və ya 60 dəqiqə. */
    @Builder.Default
    @Column(nullable = false)
    private Integer durationMinutes = 60;

    /**
     * Müştərinin konki rezervasiyası taymerini əl ilə aktivləşdirmə sayı.
     * İlk aktivləşdirmə də bu limitə daxildir (maksimum 2 cəhd).
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer reservationExtensionCount = 0;

    /**
     * Konki stoku aktivləşdirmə və ya turniketdə təhvil zamanı çıxılır.
     * Bu bayraq true olduqda stok ikinci dəfə çıxılmır.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean skateStockDeducted = false;

    /** Könüllü tərəfindən konkinin hazırlanması/təhvilə hazırlanması anı. */
    private LocalDateTime skateReservedAt;

    @Column(length = 50)
    private String skateReservedBy;

    /** Frontend rəqəmsal bileti / QR kodu. Bir sifarişin bütün rezervasiyaları eyni kodu daşıyır. */
    @Column(length = 100)
    private String voucherCode;

    /** İştirakçının adı (onlayn bilet alışında doldurulur). */
    @Column(length = 100)
    private String participantName;

    /** İştirakçının yaşı (onlayn bilet alışında doldurulur). */
    private Integer participantAge;

    /** İştirakçının şəxsiyyət vəsiqəsi ilə foto təsdiqi (base64 data URL). */
    @Column(columnDefinition = "TEXT")
    private String identityPhotoData;

    /** Tələbə endirimi seçildikdə tələbə kartı ilə foto təsdiqi (base64 data URL). */
    @Column(columnDefinition = "TEXT")
    private String studentCardPhotoData;

    /** Bu iştirakçıya tələbə endirimi tətbiq olunubmu. */
    private Boolean studentDiscountApplied;

    /** Ailə endirimi seçildikdə 3 şəxsiyyət vəsiqəsinin yan-yana fotosu. */
    @Column(columnDefinition = "TEXT")
    private String familyIdsPhotoData;

    /** PUSH30 endirimi seçildikdə Premium paket ekran şəkli. */
    @Column(columnDefinition = "TEXT")
    private String push30ScreenshotData;

    /** PUSH30 endirimi üçün istifadə edilən qeydiyyat telefon nömrəsi (tək səfərlik). */
    @Column(length = 32)
    private String push30Phone;

    /** Kassada (Kassa QR) təsdiqləndiyi an. */
    private LocalDateTime cashierVerifiedAt;

    @Column(length = 50)
    private String cashierVerifiedBy;

    private LocalDateTime checkedInAt;

    @Column(length = 50)
    private String checkedInBy;

    /** Konki rezervasiyası müştəriyə təhvil verildikdə doldurulur. */
    private LocalDateTime skateHandedOutAt;

    @Column(length = 50)
    private String skateHandedOutBy;

    /** Köhnə frontend/API uyğunluğu üçün. */
    @Deprecated
    public Integer getExtensionCount() {
        return reservationExtensionCount;
    }

    /** Köhnə frontend/API uyğunluğu üçün. */
    @Deprecated
    public void setExtensionCount(Integer extensionCount) {
        this.reservationExtensionCount = extensionCount;
    }

    public enum Status {
        PENDING, CONFIRMED, CANCELLED, EXPIRED
    }
}