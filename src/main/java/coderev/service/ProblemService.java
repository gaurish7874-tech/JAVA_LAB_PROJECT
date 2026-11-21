package coderev.service;

import coderev.dao.ProblemDao;
import coderev.exception.DuplicateProblemException;
import coderev.exception.PersistenceException;
import coderev.exception.ProblemNotFoundException;
import coderev.exception.ValidationException;
import coderev.model.ConfidenceLevel;
import coderev.model.Difficulty;
import coderev.model.Problem;
import coderev.util.AppLogger;
import coderev.util.Validators;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/** Application service for managing the problem collection. */
public class ProblemService {
    private static final Logger LOG = AppLogger.getLogger(ProblemService.class);

    private final ProblemDao problemDao;
    private final RevisionScheduler scheduler;

    public ProblemService(ProblemDao problemDao, RevisionScheduler scheduler) {
        this.problemDao = problemDao;
        this.scheduler = scheduler;
    }

    /** Validates and stores a newly solved problem, and schedules its first revision. */
    public Problem addProblem(String title, String platform, String topic, Difficulty difficulty,
                              int confidenceLevel, LocalDate dateSolved,
                              String approachNotes, String url)
            throws ValidationException, DuplicateProblemException, PersistenceException {
        Problem p = new Problem();
        p.setTitle(Validators.requireNonBlank(title, "Title"));
        p.setPlatform(Validators.requireNonBlank(platform, "Platform"));
        p.setTopic(Validators.requireNonBlank(topic, "Topic"));
        if (difficulty == null) {
            throw new ValidationException("Difficulty must be EASY, MEDIUM or HARD.");
        }
        p.setDifficulty(difficulty);
        p.setConfidence(ConfidenceLevel.fromLevel(
                Validators.requireRange(confidenceLevel, 1, 5, "Confidence")));
        p.setDateSolved(Validators.requireNotFuture(dateSolved, "Date solved"));
        p.setApproachNotes(approachNotes == null ? "" : approachNotes.trim());
        p.setUrl(url == null ? "" : url.trim());
        p.setRevisionCount(0);

        if (problemDao.findByTitleAndPlatform(p.getTitle(), p.getPlatform()).isPresent()) {
            throw new DuplicateProblemException(p.getTitle(), p.getPlatform());
        }
        p.setNextRevision(scheduler.computeNextRevision(p, p.getConfidence(), p.getDateSolved()));
        Problem saved = problemDao.insert(p);
        LOG.info("Added problem #" + saved.getId() + " '" + saved.getTitle() + "', first revision on "
                + saved.getNextRevision());
        return saved;
    }

    public void deleteProblem(long id) throws ProblemNotFoundException, PersistenceException {
        findOrThrow(id);
        problemDao.delete(id);
        LOG.info("Deleted problem #" + id);
    }

    public Problem getProblem(long id) throws ProblemNotFoundException, PersistenceException {
        return findOrThrow(id);
    }

    public List<Problem> listAll() throws PersistenceException {
        return problemDao.findAll();
    }

    public List<Problem> search(String topic, Difficulty difficulty, Integer confidenceLevel,
                                String titleContains) throws PersistenceException {
        return problemDao.search(topic, difficulty, confidenceLevel, titleContains);
    }

    public List<String> listTopics() throws PersistenceException {
        return problemDao.findDistinctTopics();
    }

    private Problem findOrThrow(long id) throws ProblemNotFoundException, PersistenceException {
        return problemDao.findById(id).orElseThrow(() -> new ProblemNotFoundException(id));
    }
}
