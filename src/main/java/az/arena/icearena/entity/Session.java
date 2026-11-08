// File: src/main/java/az/arena/icearena/entity/Session.java
package az.arena.icearena.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer maxTicketLimit;

    @Column(nullable = false)
    private Integer availableTickets;

    @Builder.Default
    @Column(nullable = false)
    private Boolean blocked = false;

    @Column(length = 250)
    private String blockReason;

    @Builder.Default
    @OneToMany(mappedBy = "session")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Ticket> tickets = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "session")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SkateRental> skateRentals = new HashSet<>();
}
