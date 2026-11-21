package coderev.model;

/** Self-assessed confidence (1-5) after solving or revising a problem.
 *  Each level carries the base interval (in days) before the next revision. */
public enum ConfidenceLevel {
    VERY_LOW(1, 1),
    LOW(2, 2),
    MEDIUM(3, 4),
    HIGH(4, 7),
    VERY_HIGH(5, 14);

    private final int level;
    private final int baseIntervalDays;

    ConfidenceLevel(int level, int baseIntervalDays) {
        this.level = level;
        this.baseIntervalDays = baseIntervalDays;
    }

    public int getLevel() { return level; }

    public int getBaseIntervalDays() { return baseIntervalDays; }

    /** How urgently this level asks for revision: 5 (very low) down to 1 (very high). */
    public int getConfidenceNeed() { return 6 - level; }

    public static ConfidenceLevel fromLevel(int level) {
        for (ConfidenceLevel c : values()) {
            if (c.level == level) {
                return c;
            }
        }
        throw new IllegalArgumentException("Confidence level must be between 1 and 5, got: " + level);
    }
}
