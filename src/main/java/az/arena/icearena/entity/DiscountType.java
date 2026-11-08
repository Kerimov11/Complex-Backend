package az.arena.icearena.entity;

/**
 * Avtomatik qiymət hesablamasında istifadə olunan endirim növləri.
 * Qrup və 5+1 endirimləri istifadəçi seçimi olmadan, iştirakçı sayına görə tətbiq olunur.
 */
public enum DiscountType {
    NONE,
    GROUP,
    FIVE_PLUS_ONE,
    STUDENT,
    FAMILY,
    PUSH30,
    QAZI,
    SHAHEED,
    SUNDAY_MORNING
}
