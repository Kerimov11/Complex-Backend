// File: src/main/java/az/arena/icearena/mapper/SkateRentalMapper.java
package az.arena.icearena.mapper;

import az.arena.icearena.dto.SkateRentalDto;
import az.arena.icearena.entity.SkateRental;
import org.springframework.stereotype.Component;

@Component
public class SkateRentalMapper {
    public SkateRentalDto toDto(SkateRental skateRental) {
        if (skateRental == null) {
            return null;
        }
        return SkateRentalDto.builder()
                .id(skateRental.getId())
                .skateSize(skateRental.getSkateSize())
                .quantity(skateRental.getQuantity())
                .pricePerPair(skateRental.getPricePerPair())
                .totalPrice(skateRental.getTotalPrice())
                .rentalDate(skateRental.getRentalDate())
                .sessionId(skateRental.getSession() != null ? skateRental.getSession().getId() : null)
                .username(skateRental.getUser() != null ? skateRental.getUser().getUsername() : null)
                .build();
    }
}
