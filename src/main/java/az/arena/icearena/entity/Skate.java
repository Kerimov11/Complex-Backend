package az.arena.icearena.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer size; // Konki ölçüsü (məsələn 38, 39, 40...)

    @Column(nullable = false)
    private Integer totalQuantity; // Ümumi say

    @Column(nullable = false)
    private Integer availableQuantity; // Hazırda boşda olan say

    @Builder.Default
    @Column(nullable = false)
    private Integer damagedQuantity = 0; // Zədələnib inventardan çıxarılan say
}