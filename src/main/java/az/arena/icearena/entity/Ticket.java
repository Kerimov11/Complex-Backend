// File: src/main/java/az/arena/icearena/entity/Ticket.java
package az.arena.icearena.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ticketCode;

    @Column(nullable = false, unique = true, length = 150)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketType ticketType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Column(nullable = false)
    private Boolean isUsed;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PaymentMethod paymentMethod;

    @Column(length = 50)
    private String soldByUsername;

    @Column
    private Integer durationMinutes;

    /** Biletin nəzərdə tutulan giriş vaxtı. */
    private LocalTime entryTime;

    /** Kassada (Kassa QR) təsdiqləndiyi an. */
    private LocalDateTime cashierVerifiedAt;

    @Column(length = 50)
    private String cashierVerifiedBy;

    /** Turniketdə QR təsdiqləndiyi faktiki an. */
    private LocalDateTime checkedInAt;

    @Column(length = 50)
    private String checkedInBy;

    /** POS satışında müştərinin adı (user sahəsi kassiri saxlayır). */
    @Column(length = 100)
    private String customerName;

    @Column(length = 30)
    private String customerPhone;

    /** POS satış qrupu üçün ümumi çek referansı. */
    @Column(length = 100)
    private String saleReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Session session;

    @OneToOne(mappedBy = "ticket")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payment payment;
}
