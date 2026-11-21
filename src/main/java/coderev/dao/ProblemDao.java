package coderev.dao;

import coderev.exception.PersistenceException;
import coderev.model.Difficulty;
import coderev.model.Problem;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Problem-specific queries on top of the generic CRUD contract. */
public interface ProblemDao extends GenericDao<Problem, Long> {

    Optional<Problem> findByTitleAndPlatform(String title, String platform) throws PersistenceException;

    /** All problems whose next revision date is today or earlier. */
    List<Problem> findDueOnOrBefore(LocalDate date) throws PersistenceException;

    /** Filtered search; any filter may be null to mean "no restriction". */
    List<Problem> search(String topic, Difficulty difficulty, Integer confidenceLevel,
                         String titleContains) throws PersistenceException;

    List<String> findDistinctTopics() throws PersistenceException;
}
