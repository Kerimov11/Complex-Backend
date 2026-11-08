package az.arena.icearena.service.impl;

import az.arena.icearena.dto.customer.CustomerRulesAcceptanceDto;
import az.arena.icearena.entity.CustomerRulesAcceptance;
import az.arena.icearena.entity.User;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.CustomerRulesAcceptanceRepository;
import az.arena.icearena.repository.UserRepository;
import az.arena.icearena.service.CustomerRulesAcceptanceService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerRulesAcceptanceServiceImpl implements CustomerRulesAcceptanceService {

    private static final List<String> OBLIGATION_IDS = List.of(
            "rules",
            "responsibility",
            "noClaim",
            "media");

    private final CustomerRulesAcceptanceRepository acceptanceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerRulesAcceptanceDto getForUser(String username) {
        return acceptanceRepository.findByUserUsername(username)
                .map(this::toDto)
                .orElseGet(() -> toDto(null));
    }

    @Override
    @Transactional
    public CustomerRulesAcceptanceDto saveForUser(
            String username,
            Set<String> acceptedObligationIds) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı."));

        CustomerRulesAcceptance acceptance = acceptanceRepository
                .findByUserUsername(username)
                .orElseGet(() -> CustomerRulesAcceptance.builder().user(user).build());

        Set<String> accepted = acceptedObligationIds == null
                ? new LinkedHashSet<>()
                : acceptedObligationIds.stream()
                        .filter(OBLIGATION_IDS::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        acceptance.setAcceptedObligationIds(accepted);
        acceptance.setConfirmed(accepted.containsAll(OBLIGATION_IDS));
        acceptance.setUpdatedAt(LocalDateTime.now());

        return toDto(acceptanceRepository.save(acceptance));
    }

    private CustomerRulesAcceptanceDto toDto(CustomerRulesAcceptance acceptance) {
        Set<String> accepted = acceptance == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(acceptance.getAcceptedObligationIds());

        return CustomerRulesAcceptanceDto.builder()
                .acceptedObligationIds(accepted)
                .acceptedCount(accepted.size())
                .totalCount(OBLIGATION_IDS.size())
                .confirmed(accepted.containsAll(OBLIGATION_IDS))
                .updatedAt(acceptance == null ? null : acceptance.getUpdatedAt())
                .build();
    }
}
