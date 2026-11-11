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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "volunteer_shifts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_volunteer_shift_slot",
                columnNames = {"user_id", "shift_slot_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_slot_id")
    private ShiftSlot shiftSlot;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShiftAssignmentStatus status = ShiftAssignmentStatus.SELECTED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Station station;

    @Column(nullable = false)
    private LocalDateTime selectedAt;

    private LocalDateTime assignedAt;

    @Column(length = 50)
    private String assignedBy;

    private LocalDate cancelledShiftDate;

    private LocalTime cancelledStartTime;

    private LocalTime cancelledEndTime;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationMessage;
}
