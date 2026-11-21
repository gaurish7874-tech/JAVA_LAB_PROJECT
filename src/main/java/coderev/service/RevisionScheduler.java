package coderev.service;

import coderev.model.ConfidenceLevel;
import coderev.model.Problem;

import java.time.LocalDate;

/** Decides when a problem should be revised next. Implementations must be
 *  deterministic: the same problem state always yields the same schedule. */
public interface RevisionScheduler {
    /**
     * @param problem       the problem as stored before this revision
     * @param newConfidence the confidence after solving/revising
     * @param today         the effective date of the revision
     * @return the date of the next scheduled revision
     */
    LocalDate computeNextRevision(Problem problem, ConfidenceLevel newConfidence, LocalDate today);
}
