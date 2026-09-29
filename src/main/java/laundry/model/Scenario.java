package laundry.model;

/** The two scenarios the simulation can run. */
public enum Scenario {
    // timeScale: real time = simulated time x timeScale, tuned so a run
    // takes 55-65 s (the congested one has extra owner delays).
    NORMAL("Normal day", 0.66),
    CONGESTED("Congested (bonus)", 0.60);

    private final String label;
    private final double timeScale;

    Scenario(String label, double timeScale) {
        this.label = label;
        this.timeScale = timeScale;
    }

    public double timeScale() {
        return timeScale;
    }

    @Override
    public String toString() {
        return label;
    }
}
