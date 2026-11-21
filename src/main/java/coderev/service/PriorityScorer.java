package coderev.service;

import coderev.model.Problem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Explainable priority score used to pick today's recommended problem:
 *
 * score = wc x confidenceNeed + wd x difficultyWeight + wr x recencyDays + wf x revisionCount
 *
 * Weights come from config/coderev.properties. A higher score means the
 * problem needs revision more urgently.
 */
public class PriorityScorer {
    private static final int RECENCY_CAP_DAYS = 30;

    private final double weightConfidence;
    private final double weightDifficulty;
    private final double weightRecency;
    private final double weightFrequency;

    public PriorityScorer(double weightConfidence, double weightDifficulty,
                          double weightRecency, double weightFrequency) {
        this.weightConfidence = weightConfidence;
        this.weightDifficulty = weightDifficulty;
        this.weightRecency = weightRecency;
        this.weightFrequency = weightFrequency;
    }

    public double score(Problem p, LocalDate today) {
        long recencyDays = Math.min(RECENCY_CAP_DAYS,
                ChronoUnit.DAYS.between(p.getRecencyAnchor(), today));
        return weightConfidence * p.getConfidence().getConfidenceNeed()
                + weightDifficulty * p.getDifficulty().getWeight()
                + weightRecency * recencyDays
                + weightFrequency * p.getRevisionCount();
    }

    /** Human-readable breakdown, shown next to the daily recommendation. */
    public String explain(Problem p, LocalDate today) {
        long recencyDays = Math.min(RECENCY_CAP_DAYS,
                ChronoUnit.DAYS.between(p.getRecencyAnchor(), today));
        double c = weightConfidence * p.getConfidence().getConfidenceNeed();
        double d = weightDifficulty * p.getDifficulty().getWeight();
        double r = weightRecency * recencyDays;
        double f = weightFrequency * p.getRevisionCount();
        return String.format(
                "confidence need %.1f + difficulty %.1f + recency %.1f (%d days) + frequency %.1f (%d revisions) = %.1f",
                c, d, r, recencyDays, f, p.getRevisionCount(), c + d + r + f);
    }
}
