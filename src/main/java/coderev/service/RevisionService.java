package coderev.service;

import coderev.dao.Database;
import coderev.dao.JdbcProblemDao;
import coderev.dao.JdbcRevisionDao;
import coderev.dao.ProblemDao;
import coderev.dao.RevisionDao;
import coderev.exception.PersistenceException;
import coderev.exception.ProblemNotFoundException;
import coderev.exception.ValidationException;
import coderev.model.ConfidenceLevel;
import coderev.model.Problem;
import coderev.model.RevisionRecord;
import coderev.util.AppLogger;
import coderev.util.Validators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Records revisions, recomputes schedules and picks the daily recommendation. */
public class RevisionService {
    private static final Logger LOG = AppLogger.getLogger(RevisionService.class);

    private final Database db;
    private final ProblemDao problemDao;
    private final RevisionDao revisionDao;
    private final RevisionScheduler scheduler;
    private final PriorityScorer scorer;

    public RevisionService(Database db, ProblemDao problemDao, RevisionDao revisionDao,
                           RevisionScheduler scheduler, PriorityScorer scorer) {
        this.db = db;
        this.problemDao = problemDao;
        this.revisionDao = revisionDao;
        this.scheduler = scheduler;
        this.scorer = scorer;
    }

    /**
     * Records one revision in a single transaction: history insert, confidence
     * update, counters and the next scheduled date all move together.
     */
    public RevisionRecord recordRevision(long problemId, int newConfidenceLevel, int performancePercent)
            throws ValidationException, ProblemNotFoundException, PersistenceException {
        ConfidenceLevel newConfidence = ConfidenceLevel.fromLevel(
                Validators.requireRange(newConfidenceLevel, 1, 5, "Confidence"));
        Validators.requireRange(performancePercent, 0, 100, "Performance");

        return db.inTransaction(conn -> {
            JdbcProblemDao pDao = (JdbcProblemDao) problemDao;
            JdbcRevisionDao rDao = (JdbcRevisionDao) revisionDao;

            Problem p = pDao.findById(problemId)
                    .orElseThrow(() -> new ProblemNotFoundException(problemId));

            RevisionRecord record = new RevisionRecord(
                    problemId, LocalDateTime.now(), p.getConfidence(), newConfidence,
                    p.getRevisionCount() + 1, performancePercent);
            rDao.insert(conn, record);

            LocalDate today = LocalDate.now();
            p.setLastRevised(today);
            p.setConfidence(newConfidence);
            p.setRevisionCount(p.getRevisionCount() + 1);
            p.setNextRevision(scheduler.computeNextRevision(p, newConfidence, today));
            pDao.update(conn, p);

            LOG.info("Revision #" + record.getRevisionNumber() + " for problem " + problemId
                    + ", next revision on " + p.getNextRevision());
            return record;
        });
    }

    /** All problems due today or overdue, highest priority first. */
    public List<Problem> dueToday() throws PersistenceException {
        LocalDate today = LocalDate.now();
        List<Problem> due = problemDao.findDueOnOrBefore(today);
        due.sort(Comparator.comparingDouble((Problem p) -> scorer.score(p, today)).reversed());
        return due;
    }

    /** The single most urgent due problem, with an explainable reason. */
    public Optional<Recommendation> recommendToday() throws PersistenceException {
        LocalDate today = LocalDate.now();
        return dueToday().stream()
                .findFirst()
                .map(p -> new Recommendation(p, scorer.score(p, today), scorer.explain(p, today)));
    }

    public List<RevisionRecord> historyFor(long problemId) throws PersistenceException {
        return revisionDao.findByProblemId(problemId);
    }

    /** Full revision history across all problems, oldest first. */
    public List<RevisionRecord> allHistory() throws PersistenceException {
        return revisionDao.findAll();
    }

    /** Immutable recommendation: the problem, its score and why it was picked. */
    public static final class Recommendation {
        private final Problem problem;
        private final double score;
        private final String explanation;

        public Recommendation(Problem problem, double score, String explanation) {
            this.problem = problem;
            this.score = score;
            this.explanation = explanation;
        }

        public Problem getProblem() { return problem; }
        public double getScore() { return score; }
        public String getExplanation() { return explanation; }
    }
}
