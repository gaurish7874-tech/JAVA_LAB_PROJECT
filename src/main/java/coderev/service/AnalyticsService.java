package coderev.service;

import coderev.dao.ProblemDao;
import coderev.dao.RevisionDao;
import coderev.exception.PersistenceException;
import coderev.model.ConfidenceLevel;
import coderev.model.Difficulty;
import coderev.model.Problem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Topic-, difficulty- and confidence-level statistics over the collection. */
public class AnalyticsService {
    private final ProblemDao problemDao;
    private final RevisionDao revisionDao;

    public AnalyticsService(ProblemDao problemDao, RevisionDao revisionDao) {
        this.problemDao = problemDao;
        this.revisionDao = revisionDao;
    }

    public Summary summary() throws PersistenceException {
        List<Problem> all = problemDao.findAll();
        LocalDate today = LocalDate.now();

        Map<String, TopicStats> byTopic = new TreeMap<>();
        Map<Difficulty, Integer> byDifficulty = new EnumMap<>(Difficulty.class);
        Map<ConfidenceLevel, Integer> byConfidence = new EnumMap<>(ConfidenceLevel.class);
        int due = 0;

        for (Problem p : all) {
            TopicStats ts = byTopic.computeIfAbsent(p.getTopic(), t -> new TopicStats(t));
            ts.accept(p);
            byDifficulty.merge(p.getDifficulty(), 1, Integer::sum);
            byConfidence.merge(p.getConfidence(), 1, Integer::sum);
            if (p.getNextRevision() != null && !p.getNextRevision().isAfter(today)) {
                due++;
            }
        }

        long totalRevisions = revisionDao.count();
        return new Summary(all.size(), due, totalRevisions,
                new ArrayList<>(byTopic.values()), byDifficulty, byConfidence);
    }

    /** Per-topic aggregate: count, average confidence, weakest topic detection. */
    public static final class TopicStats {
        private final String topic;
        private int count;
        private int confidenceSum;
        private int due;

        TopicStats(String topic) {
            this.topic = topic;
        }

        void accept(Problem p) {
            count++;
            confidenceSum += p.getConfidence().getLevel();
            if (p.getNextRevision() != null && !p.getNextRevision().isAfter(LocalDate.now())) {
                due++;
            }
        }

        public String getTopic() { return topic; }
        public int getCount() { return count; }
        public int getDue() { return due; }

        public double getAverageConfidence() {
            return count == 0 ? 0 : (double) confidenceSum / count;
        }
    }

    /** Whole-collection snapshot for dashboards and reports. */
    public static final class Summary {
        private final int totalProblems;
        private final int dueToday;
        private final long totalRevisions;
        private final List<TopicStats> topicStats;
        private final Map<Difficulty, Integer> byDifficulty;
        private final Map<ConfidenceLevel, Integer> byConfidence;

        Summary(int totalProblems, int dueToday, long totalRevisions, List<TopicStats> topicStats,
                Map<Difficulty, Integer> byDifficulty, Map<ConfidenceLevel, Integer> byConfidence) {
            this.totalProblems = totalProblems;
            this.dueToday = dueToday;
            this.totalRevisions = totalRevisions;
            this.topicStats = topicStats;
            this.byDifficulty = byDifficulty;
            this.byConfidence = byConfidence;
        }

        public int getTotalProblems() { return totalProblems; }
        public int getDueToday() { return dueToday; }
        public long getTotalRevisions() { return totalRevisions; }
        public List<TopicStats> getTopicStats() { return topicStats; }
        public Map<Difficulty, Integer> getByDifficulty() { return byDifficulty; }
        public Map<ConfidenceLevel, Integer> getByConfidence() { return byConfidence; }

        /** Topic with the lowest average confidence (null when the collection is empty). */
        public TopicStats weakestTopic() {
            TopicStats weakest = null;
            for (TopicStats ts : topicStats) {
                if (weakest == null || ts.getAverageConfidence() < weakest.getAverageConfidence()) {
                    weakest = ts;
                }
            }
            return weakest;
        }
    }
}
