package coderev.model;

/** Difficulty of a solved problem. The weight feeds the priority score and
 *  the scheduling multiplier (harder problems are revised sooner). */
public enum Difficulty {
    EASY(1, 1.25),
    MEDIUM(2, 1.00),
    HARD(3, 0.75);

    private final int weight;
    private final double intervalMultiplier;

    Difficulty(int weight, double intervalMultiplier) {
        this.weight = weight;
        this.intervalMultiplier = intervalMultiplier;
    }

    public int getWeight() { return weight; }

    /** Multiplier applied to the base revision interval. */
    public double getIntervalMultiplier() { return intervalMultiplier; }

    public static Difficulty fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Difficulty must not be null");
        }
        return Difficulty.valueOf(value.trim().toUpperCase());
    }
}
