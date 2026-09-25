package laundry.model;

/** The two scenarios the simulation can run. */
public enum Scenario {
    NORMAL("Normal day"),
    CONGESTED("Congested - both kiosks broken (bonus)");

    private final String label;

    Scenario(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
