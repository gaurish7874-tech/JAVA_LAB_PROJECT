package coderev.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** A coding problem the user has solved and wants to keep revising. */
public class Problem implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String title;
    private String platform;          // e.g. LeetCode, Codeforces
    private String topic;             // e.g. Dynamic Programming
    private Difficulty difficulty;
    private ConfidenceLevel confidence;
    private LocalDate dateSolved;
    private LocalDate lastRevised;    // null until the first revision
    private LocalDate nextRevision;   // scheduled by the RevisionScheduler
    private int revisionCount;
    private String approachNotes;     // how the user solved it
    private String url;               // link to the problem statement

    public Problem() {
    }

    public Problem(String title, String platform, String topic, Difficulty difficulty,
                   ConfidenceLevel confidence, LocalDate dateSolved, String approachNotes, String url) {
        this.title = title;
        this.platform = platform;
        this.topic = topic;
        this.difficulty = difficulty;
        this.confidence = confidence;
        this.dateSolved = dateSolved;
        this.approachNotes = approachNotes;
        this.url = url;
        this.revisionCount = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public ConfidenceLevel getConfidence() { return confidence; }
    public void setConfidence(ConfidenceLevel confidence) { this.confidence = confidence; }
    public LocalDate getDateSolved() { return dateSolved; }
    public void setDateSolved(LocalDate dateSolved) { this.dateSolved = dateSolved; }
    public LocalDate getLastRevised() { return lastRevised; }
    public void setLastRevised(LocalDate lastRevised) { this.lastRevised = lastRevised; }
    public LocalDate getNextRevision() { return nextRevision; }
    public void setNextRevision(LocalDate nextRevision) { this.nextRevision = nextRevision; }
    public int getRevisionCount() { return revisionCount; }
    public void setRevisionCount(int revisionCount) { this.revisionCount = revisionCount; }
    public String getApproachNotes() { return approachNotes; }
    public void setApproachNotes(String approachNotes) { this.approachNotes = approachNotes; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    /** Effective date the recency term is measured from. */
    public LocalDate getRecencyAnchor() {
        return lastRevised != null ? lastRevised : dateSolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Problem)) return false;
        Problem other = (Problem) o;
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " [" + topic + ", " + difficulty + ", confidence "
                + (confidence == null ? "?" : confidence.getLevel()) + "]";
    }
}
