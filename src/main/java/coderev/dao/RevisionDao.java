package coderev.dao;

import coderev.exception.PersistenceException;
import coderev.model.RevisionRecord;

import java.util.List;

/** Persistence for revision history entries. */
public interface RevisionDao {
    RevisionRecord insert(RevisionRecord record) throws PersistenceException;

    List<RevisionRecord> findByProblemId(long problemId) throws PersistenceException;

    List<RevisionRecord> findAll() throws PersistenceException;

    long count() throws PersistenceException;
}
