package az.arena.icearena.service.impl;

import az.arena.icearena.dto.DiscountBreakdownDto;
import az.arena.icearena.dto.DiscountRequest;
import az.arena.icearena.dto.DiscountRuleDto;
import az.arena.icearena.dto.PriceLineDto;
import az.arena.icearena.entity.DiscountRule;
import az.arena.icearena.entity.DiscountType;
import az.arena.icearena.entity.Session;
import az.arena.icearena.exception.BadRequestException;
import az.arena.icearena.exception.ResourceNotFoundException;
import az.arena.icearena.repository.DiscountRuleRepository;
import az.arena.icearena.repository.SessionRepository;
import az.arena.icearena.service.DiscountService;
import az.arena.icearena.service.SessionTimeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avtomatik endirim mühərriki.
 *
 * Qrup (10+) və 5+1 endirimləri istifadəçi seçimi olmadan iştirakçı sayına
 * görə hesablanır. Eyni anda yalnız bir endirim tətbiq olunur — müştəri üçün
 * ən sərfəli variant seçilir.
 */
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private static final BigDecimal WEEKDAY_HOUR = new BigDecimal("20.00");
    private static final BigDecimal WEEKDAY_HALF = new BigDecimal("15.00");
    private static final BigDecimal WEEKEND_HOUR = new BigDecimal("25.00");
    private static final BigDecimal WEEKEND_HALF = new BigDecimal("20.00");
    private static final int HOUR = 60;
    private static final int HALF = 30;

    private final DiscountRuleRepository discountRuleRepository;
    private final SessionRepository sessionRepository;
    private final SessionTimeService sessionTimeService;

    @Override
    @Transactional(readOnly = true)
    public DiscountBreakdownDto calculate(DiscountRequest request) {
        validateRequest(request);

        boolean isHour = request.getDurationMinutes() == HOUR;
        String dayType = isWeekend(request.getDate()) ? "weekend" : "weekday";
        BigDecimal unit = isHour
                ? ("weekend".equals(dayType) ? WEEKEND_HOUR : WEEKDAY_HOUR)
                : ("weekend".equals(dayType) ? WEEKEND_HALF : WEEKDAY_HALF);
        int qty = request.getQuantity();
        BigDecimal original = money(unit.multiply(BigDecimal.valueOf(qty)));

        DiscountRule groupRule = rule(DiscountType.GROUP, new BigDecimal("20.00"), 10, null, HOUR);
        DiscountRule fiveRule = rule(DiscountType.FIVE_PLUS_ONE, null, 6, 1, null);
        DiscountRule studentRule = rule(DiscountType.STUDENT, new BigDecimal("20.00"), 1, null, HOUR);
        DiscountRule familyRule = rule(DiscountType.FAMILY, null, 2, 1, null);
        DiscountRule pushRule = rule(DiscountType.PUSH30, null, 1, 1, HOUR);
        DiscountRule qaziRule = rule(DiscountType.QAZI, new BigDecimal("50.00"), 1, 1, HOUR);
        DiscountRule shaheedRule = rule(DiscountType.SHAHEED, null, 1, 50, null);
        DiscountRule sundayRule = rule(DiscountType.SUNDAY_MORNING, new BigDecimal("20.00"), 1, null, HOUR);

        List<Candidate> candidates = new ArrayList<>();

        // 1) Şəhid ailəsi — bütün biletlər pulsuz (təsdiq tələb olunur).
        if (Boolean.TRUE.equals(request.getShaheed())
                && Boolean.TRUE.equals(request.getShaheedVerified())) {
            int free = Math.max(1, value(shaheedRule.getFreeTickets(), 1));
            candidates.add(freeCandidate(DiscountType.SHAHEED,
                    "Şəhid ailələri — pulsuz giriş", unit, qty, free));
        }

        // 2) Qazi — ilk giriş pulsuz, 2-ci giriş 50%.
        if (Boolean.TRUE.equals(request.getQazi()) && isHour) {
            int visits = value(request.getQaziVisits(), 0);
            if (visits == 0) {
                candidates.add(freeCandidate(DiscountType.QAZI,
                        "Qazi — ilk giriş pulsuz", unit, qty,
                        value(qaziRule.getFreeTickets(), 1)));
            } else if (visits == 1) {
                candidates.add(percentCandidate(DiscountType.QAZI,
                        "Qazi — 2-ci giriş 50% endirim", unit, qty,
                        percent(qaziRule.getPercent(), new BigDecimal("50.00"))));
            }
        }

        // 3) PUSH30 — 1 saatlıq bilet üçün 1 pulsuz giriş.
        if (Boolean.TRUE.equals(request.getPush30()) && isHour) {
            candidates.add(freeCandidate(DiscountType.PUSH30,
                    "PUSH30 — premium paket, 1 giriş pulsuz", unit, qty,
                    value(pushRule.getFreeTickets(), 1)));
        }

        // 4) Ailə — 2+ nəfər olduqda 1 nəfər pulsuz.
        if (Boolean.TRUE.equals(request.getFamily())
                && qty >= value(familyRule.getMinParticipants(), 2)) {
            candidates.add(freeCandidate(DiscountType.FAMILY,
                    "Ailə endirimi — 1 nəfər pulsuz", unit, qty,
                    value(familyRule.getFreeTickets(), 1)));
        }

        // 5) 5+1 — hər 6 nəfərdən 1-i pulsuz. 12 nəfər = 2 pulsuz.
        int fiveMin = value(fiveRule.getMinParticipants(), 6);
        if (qty >= fiveMin) {
            int freePerBlock = Math.max(1, value(fiveRule.getFreeTickets(), 1));
            int free = Math.max(1, (qty / fiveMin) * freePerBlock);
            candidates.add(freeCandidate(DiscountType.FIVE_PLUS_ONE,
                    "5+1 kampaniyası — " + free + " nəfər pulsuz", unit, qty, free));
        }

        // 6) Qrup — 10+ nəfər və 1 saatlıq bilet.
        int groupMin = value(groupRule.getMinParticipants(), 10);
        boolean groupEligible = isHour && qty >= groupMin;
        if (groupEligible) {
            candidates.add(percentCandidate(DiscountType.GROUP,
                    "Qrup endirimi — " + qty + " nəfər, "
                            + percent(groupRule.getPercent(), new BigDecimal("20.00")).stripTrailingZeros()
                            .toPlainString() + "%",
                    unit, qty, percent(groupRule.getPercent(), new BigDecimal("20.00"))));
        }

        // 7) Tələbə — yalnız həftəiçi və 1 saatlıq bilet.
        if (Boolean.TRUE.equals(request.getStudent())
                && isHour
                && request.getDate().getDayOfWeek() != DayOfWeek.SATURDAY
                && request.getDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
            int sCount = request.getStudentCount() == null
                    ? qty
                    : Math.max(0, Math.min(qty, request.getStudentCount()));
            BigDecimal studentPct = percent(studentRule.getPercent(), new BigDecimal("20.00"));
            String pctText = studentPct.stripTrailingZeros().toPlainString();
            if (sCount >= qty) {
                candidates.add(percentCandidate(DiscountType.STUDENT,
                        "Tələbə endirimi — həftəiçi " + pctText + "%",
                        unit, qty, studentPct));
            } else if (sCount > 0) {
                BigDecimal factor = BigDecimal.ONE.subtract(
                        studentPct.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                BigDecimal studentUnit = money(unit.multiply(factor));
                BigDecimal studentTotal = money(studentUnit.multiply(BigDecimal.valueOf(sCount)));
                int regularCount = qty - sCount;
                BigDecimal regularTotal = money(unit.multiply(BigDecimal.valueOf(regularCount)));
                BigDecimal finalStudentPrice = money(studentTotal.add(regularTotal));
                List<PriceLineDto> studentLines = new ArrayList<>();
                studentLines.add(PriceLineDto.builder()
                        .label("Tələbə girişi (" + pctText + "% endirim)")
                        .quantity(sCount)
                        .unitPrice(studentUnit)
                        .total(studentTotal)
                        .build());
                studentLines.add(PriceLineDto.builder()
                        .label("Standart giriş")
                        .quantity(regularCount)
                        .unitPrice(unit)
                        .total(regularTotal)
                        .build());
                candidates.add(new Candidate(
                        DiscountType.STUDENT,
                        "Tələbə endirimi — " + sCount + "/" + qty + " nəfər (" + pctText + "%)",
                        finalStudentPrice,
                        0,
                        unit,
                        studentLines));
            }
        }

        // 8) Bazar günü 10:00–12:00 arası 1 saatlıq biletə 20%.
        if (isHour
                && request.getDate().getDayOfWeek() == DayOfWeek.SUNDAY
                && !request.getStartTime().isBefore(LocalTime.of(10, 0))
                && !request.getStartTime().isAfter(LocalTime.of(12, 0))) {
            candidates.add(percentCandidate(DiscountType.SUNDAY_MORNING,
                    "Bazar günü 10:00–12:00 — 20%", unit, qty,
                    percent(sundayRule.getPercent(), new BigDecimal("20.00"))));
        }

        Candidate best = candidates.stream()
                .min(Comparator.comparing(Candidate::finalPrice)
                        .thenComparing(candidate -> candidate.freeTickets() > 0 ? 0 : 1))
                .orElse(null);

        List<String> notes = new ArrayList<>();
        if (!isHour) {
            notes.add("Yarım saatlıq biletlərə faiz endirimləri tətbiq olunmur.");
        }
        if (qty >= groupMin && !isHour) {
            notes.add("Qrup endirimi yalnız 1 saatlıq biletlərə aiddir.");
        }
        if (qty >= fiveMin && best == null) {
            notes.add("5+1 kampaniyası tətbiq olunmadı.");
        }

        BigDecimal finalPrice = best == null ? original : best.finalPrice();
        BigDecimal discountAmount = money(original.subtract(finalPrice));
        BigDecimal unitPrice = best == null ? unit : best.unitPrice();
        int freeTickets = best == null ? 0 : best.freeTickets();
        String appliedType = best == null ? DiscountType.NONE.name() : best.type().name();
        String appliedLabel = best == null ? "Endirim yoxdur" : best.label();

        List<PriceLineDto> lines = best == null
                ? List.of(PriceLineDto.builder()
                        .label("Standart vahid qiymət")
                        .quantity(qty)
                        .unitPrice(unit)
                        .total(original)
                        .build())
                : best.lines();

        DiscountBreakdownDto.DiscountBreakdownDtoBuilder builder = DiscountBreakdownDto.builder()
                .originalPrice(original)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .appliedDiscountType(appliedType)
                .appliedDiscountLabel(appliedLabel)
                .unitPrice(unitPrice)
                .quantity(qty)
                .durationMinutes(request.getDurationMinutes())
                .freeTickets(freeTickets)
                .dayType(dayType)
                .groupEligible(groupEligible)
                .fivePlusOneEligible(qty >= fiveMin)
                .lines(lines)
                .notes(notes);

        // Sessiya verilibsə, cavaba dinamik qalıq vaxtı da əlavə edirik.
        if (request.getSessionId() != null) {
            Optional<Session> session = sessionRepository.findById(request.getSessionId());
            if (session.isPresent()) {
                var availability = sessionTimeService.evaluate(
                        session.get(), request.getStartTime(), request.getDurationMinutes());
                builder.remainingMinutes(availability.getRemainingMinutes())
                        .salesOpen(availability.getSalesOpen())
                        .availableDurations(availability.getAvailableDurations());
            }
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountRuleDto> getActiveRules() {
        return discountRuleRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(rule -> rule.getType().ordinal()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountRuleDto> getAllRules() {
        return discountRuleRepository.findAll().stream()
                .sorted(Comparator.comparing(rule -> rule.getType().ordinal()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DiscountRuleDto createRule(DiscountRuleDto dto) {
        if (dto.getType() == null) {
            throw new BadRequestException("Endirim növü tələb olunur.");
        }
        if (discountRuleRepository.findByType(dto.getType()).isPresent()) {
            throw new BadRequestException("Bu endirim növü artıq mövcuddur.");
        }
        DiscountRule rule = DiscountRule.builder()
                .type(dto.getType())
                .title(dto.getTitle() == null ? dto.getType().name() : dto.getTitle())
                .description(dto.getDescription())
                .percent(dto.getPercent())
                .minParticipants(dto.getMinParticipants())
                .freeTickets(dto.getFreeTickets())
                .durationMinutes(dto.getDurationMinutes())
                .active(dto.getActive() == null || dto.getActive())
                .build();
        return toDto(discountRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public DiscountRuleDto updateRule(Long id, DiscountRuleDto dto) {
        DiscountRule rule = discountRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount rule not found with id: " + id));
        if (dto.getType() != null) {
            rule.setType(dto.getType());
        }
        if (dto.getTitle() != null) {
            rule.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            rule.setDescription(dto.getDescription());
        }
        if (dto.getPercent() != null) {
            rule.setPercent(dto.getPercent());
        }
        if (dto.getMinParticipants() != null) {
            rule.setMinParticipants(dto.getMinParticipants());
        }
        if (dto.getFreeTickets() != null) {
            rule.setFreeTickets(dto.getFreeTickets());
        }
        if (dto.getDurationMinutes() != null) {
            rule.setDurationMinutes(dto.getDurationMinutes());
        }
        if (dto.getActive() != null) {
            rule.setActive(dto.getActive());
        }
        return toDto(discountRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        if (!discountRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Discount rule not found with id: " + id);
        }
        discountRuleRepository.deleteById(id);
    }

    private void validateRequest(DiscountRequest request) {
        if (request == null || request.getDate() == null || request.getStartTime() == null
                || request.getDurationMinutes() == null || request.getQuantity() == null) {
            throw new BadRequestException("Tarix, başlama vaxtı, müddət və iştirakçı sayı tələb olunur.");
        }
        if (request.getDurationMinutes() != HALF && request.getDurationMinutes() != HOUR) {
            throw new BadRequestException("Bilet müddəti yalnız 30 və ya 60 dəqiqə ola bilər.");
        }
        if (request.getQuantity() < 1 || request.getQuantity() > 1000) {
            throw new BadRequestException("İştirakçı sayı 1–1000 aralığında olmalıdır.");
        }
    }

    private DiscountRule rule(DiscountType type,
                              BigDecimal defaultPercent,
                              Integer defaultMin,
                              Integer defaultFree,
                              Integer defaultDuration) {
        return discountRuleRepository.findByType(type)
                .orElseGet(() -> DiscountRule.builder()
                        .type(type)
                        .title(type.name())
                        .percent(defaultPercent)
                        .minParticipants(defaultMin)
                        .freeTickets(defaultFree)
                        .durationMinutes(defaultDuration)
                        .active(true)
                        .build());
    }

    private Candidate percentCandidate(DiscountType type,
                                       String label,
                                       BigDecimal unit,
                                       int qty,
                                       BigDecimal percent) {
        BigDecimal safePercent = percent == null ? BigDecimal.ZERO : percent;
        BigDecimal multiplier = BigDecimal.valueOf(100).subtract(safePercent)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal discountedUnit = money(unit.multiply(multiplier));
        BigDecimal finalPrice = money(discountedUnit.multiply(BigDecimal.valueOf(qty)));
        return new Candidate(
                type,
                label,
                finalPrice,
                0,
                discountedUnit,
                List.of(PriceLineDto.builder()
                        .label("Endirimli vahid qiymət")
                        .quantity(qty)
                        .unitPrice(discountedUnit)
                        .total(finalPrice)
                        .build())
        );
    }

    private Candidate freeCandidate(DiscountType type,
                                    String label,
                                    BigDecimal unit,
                                    int qty,
                                    int requestedFree) {
        int free = Math.min(qty, Math.max(0, requestedFree));
        int paying = Math.max(0, qty - free);
        BigDecimal finalPrice = money(unit.multiply(BigDecimal.valueOf(paying)));
        List<PriceLineDto> lines = new ArrayList<>();
        if (paying > 0) {
            lines.add(PriceLineDto.builder()
                    .label("Ödənişli giriş")
                    .quantity(paying)
                    .unitPrice(unit)
                    .total(money(unit.multiply(BigDecimal.valueOf(paying))))
                    .build());
        }
        if (free > 0) {
            lines.add(PriceLineDto.builder()
                    .label("Pulsuz giriş")
                    .quantity(free)
                    .unitPrice(BigDecimal.ZERO.setScale(2))
                    .total(BigDecimal.ZERO.setScale(2))
                    .build());
        }
        return new Candidate(type, label, finalPrice, free, unit, lines);
    }

    private DiscountRuleDto toDto(DiscountRule rule) {
        return DiscountRuleDto.builder()
                .id(rule.getId())
                .type(rule.getType())
                .title(rule.getTitle())
                .description(rule.getDescription())
                .percent(rule.getPercent())
                .minParticipants(rule.getMinParticipants())
                .freeTickets(rule.getFreeTickets())
                .durationMinutes(rule.getDurationMinutes())
                .active(rule.getActive())
                .build();
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private BigDecimal percent(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record Candidate(DiscountType type,
                             String label,
                             BigDecimal finalPrice,
                             int freeTickets,
                             BigDecimal unitPrice,
                             List<PriceLineDto> lines) {
    }
}
