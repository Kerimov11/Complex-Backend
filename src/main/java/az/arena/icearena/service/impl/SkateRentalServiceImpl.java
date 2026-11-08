// File: src/main/java/az/arena/icearena/service/impl/SkateRentalServiceImpl.java
package az.arena.icearena.service.impl;

import az.arena.icearena.dto.SkateRentalDto;
import az.arena.icearena.dto.SkateRentalRequest;
import az.arena.icearena.entity.Session;
import az.arena.icearena.entity.SkateRental;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.mapper.SkateRentalMapper;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.repository.SkateRentalRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.PaymentService;
import az.arena.icearena.service.SkateRentalService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkateRentalServiceImpl implements SkateRentalService {

    private final SkateRentalRepository skateRentalRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SkateRentalMapper skateRentalMapper;
    private final PaymentService paymentService;

    private static final BigDecimal PRICE_PER_PAIR = new BigDecimal("8.00");

    @Override
    @Transactional
    public SkateRentalDto rentSkates(SkateRentalRequest request, String username) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + request.getSessionId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal totalPrice = PRICE_PER_PAIR.multiply(BigDecimal.valueOf(request.getQuantity()));

        SkateRental skateRental = SkateRental.builder()
                .skateSize(request.getSkateSize())
                .quantity(request.getQuantity())
                .pricePerPair(PRICE_PER_PAIR)
                .totalPrice(totalPrice)
                .rentalDate(LocalDateTime.now())
                .user(user)
                .session(session)
                .build();

        SkateRental savedSkateRental = skateRentalRepository.save(skateRental);
        paymentService.createPaymentForSkateRental(savedSkateRental.getId(), totalPrice);

        return skateRentalMapper.toDto(savedSkateRental);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkateRentalDto> getUserSkateRentals(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return skateRentalRepository.findByUserId(user.getId()).stream()
                .map(skateRentalMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkateRentalDto> getSkateRentalsBySession(Long sessionId) {
        return skateRentalRepository.findBySessionId(sessionId).stream()
                .map(skateRentalMapper::toDto)
                .collect(Collectors.toList());
    }
}
