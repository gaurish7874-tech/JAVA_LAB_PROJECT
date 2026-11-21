package coderev.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One revision event: when it happened, how confidence changed and how
 *  well the revision went (performance in percent, 0-100). */
public class RevisionRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long problemId;
    private LocalDateTime revisedAt;
    private ConfidenceLevel previousConfidence;
    private ConfidenceLevel newConfidence;
    private int revisionNumber;
    private int performancePercent;

    public RevisionRecord() {
    }

    public RevisionRecord(long problemId, LocalDateTime revisedAt, ConfidenceLevel previousConfidence,
                          ConfidenceLevel newConfidence, int revisionNumber, int performancePercent) {
        this.problemId = problemId;
        this.revisedAt = revisedAt;
        this.previousConfidence = previousConfidence;
        this.newConfidence = newConfidence;
        this.revisionNumber = revisionNumber;
        this.performancePercent = performancePercent;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProblemId() { return problemId; }
    public void setProblemId(long problemId) { this.problemId = problemId; }
    public LocalDateTime getRevisedAt() { return revisedAt; }
    public void setRevisedAt(LocalDateTime revisedAt) { this.revisedAt = revisedAt; }
    public ConfidenceLevel getPreviousConfidence() { return previousConfidence; }
    public void setPreviousConfidence(ConfidenceLevel previousConfidence) { this.previousConfidence = previousConfidence; }
    public ConfidenceLevel getNewConfidence() { return newConfidence; }
    public void setNewConfidence(ConfidenceLevel newConfidence) { this.newConfidence = newConfidence; }
    public int getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(int revisionNumber) { this.revisionNumber = revisionNumber; }
    public int getPerformancePercent() { return performancePercent; }
    public void setPerformancePercent(int performancePercent) { this.performancePercent = performancePercent; }

    @Override
    public String toString() {
        return "Revision #" + revisionNumber + " on " + revisedAt.toLocalDate()
                + ": confidence " + (previousConfidence == null ? "-" : previousConfidence.getLevel())
                + " -> " + newConfidence.getLevel()
                + ", performance " + performancePercent + "%";
    }
}
