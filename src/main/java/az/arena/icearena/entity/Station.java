package az.arena.icearena.entity;

public enum Station {
    KASSA_TELEFON("Kassa / Telefon"),
    TURNIKET("Turniket"),
    SEKTOR("Sektor"),
    MIKRAFON("Mikrafon"),
    BUZ("Buz");

    private final String label;

    Station(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
