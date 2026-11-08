package az.arena.icearena.service;

import az.arena.icearena.dto.DiscountBreakdownDto;
import az.arena.icearena.dto.DiscountRequest;
import az.arena.icearena.dto.DiscountRuleDto;
import java.util.List;

public interface DiscountService {
    DiscountBreakdownDto calculate(DiscountRequest request);

    List<DiscountRuleDto> getActiveRules();

    List<DiscountRuleDto> getAllRules();

    DiscountRuleDto createRule(DiscountRuleDto dto);

    DiscountRuleDto updateRule(Long id, DiscountRuleDto dto);

    void deleteRule(Long id);
}
