package az.arena.icearena.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Müştərinin "Qaydalar" səhifəsində qəbul etdiyi öhdəliklərin backend saxlanması.
 * Bir istifadəçi üçün bir aktiv qeyd saxlanılır.
 */
@Entity
@Table(
        name = "customer_rules_acceptances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_rules_acceptance_user",
                columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRulesAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "customer_rules_acceptance_obligations",
            joinColumns = @JoinColumn(name = "acceptance_id"))
    @Column(name = "obligation_id", nullable = false, length = 50)
    private Set<String> acceptedObligationIds = new HashSet<>();

    @Column(nullable = false)
    private boolean confirmed;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
