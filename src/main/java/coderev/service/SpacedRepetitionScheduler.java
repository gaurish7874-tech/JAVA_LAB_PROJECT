package coderev.service;

import coderev.model.ConfidenceLevel;
import coderev.model.Problem;

import java.time.LocalDate;

/**
 * Deterministic spaced-repetition scheduler.
 *
 * interval = baseInterval(newConfidence) x difficultyMultiplier x growth(revisionCount)
 *
 * - base interval comes from the confidence level (1, 2, 4, 7 or 14 days)
 * - HARD problems shrink the interval (x0.75), EASY stretch it (x1.25)
 * - the interval grows by 50% per past revision, capped at 4 revisions,
 *   so well-known problems space out while new ones stay close
 * - the result is clamped to [1, 60] days
 */
public class SpacedRepetitionScheduler implements RevisionScheduler {
    static final int MAX_INTERVAL_DAYS = 60;

    @Override
    public LocalDate computeNextRevision(Problem problem, ConfidenceLevel newConfidence, LocalDate today) {
        double base = newConfidence.getBaseIntervalDays();
        double difficultyMultiplier = problem.getDifficulty().getIntervalMultiplier();
        double growth = 1.0 + 0.5 * Math.min(problem.getRevisionCount(), 4);
        long interval = Math.round(base * difficultyMultiplier * growth);
        interval = Math.max(1, Math.min(MAX_INTERVAL_DAYS, interval));
        return today.plusDays(interval);
    }
}
