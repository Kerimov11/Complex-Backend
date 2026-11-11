// File: src/main/java/az/arena/icearena/entity/User.java
package az.arena.icearena.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    /** Müştərinin doğum tarixi. Bilet satışı üçün 18+ yoxlaması buradan aparılır. */
    @Column
    private LocalDate dateOfBirth;

    @Column
    private LocalDateTime ageVerifiedAt;

    /** Şəxsiyyət vəsiqəsinin seriya və nömrəsi; yalnız müştəri təsdiqi üçün saxlanılır. */
    @JsonIgnore
    @ToString.Exclude
    @Column(length = 30)
    private String identityDocumentNumber;

    @Column
    private LocalDateTime identityConfirmedAt;

    @JsonIgnore
    @ToString.Exclude
    @Column(length = 12)
    private String passwordResetCode;

    @JsonIgnore
    @ToString.Exclude
    @Column
    private LocalDateTime passwordResetExpiresAt;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Ticket> tickets = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SkateRental> skateRentals = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Payment> payments = new HashSet<>();
}
