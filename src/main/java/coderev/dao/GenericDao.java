package coderev.dao;

import coderev.exception.PersistenceException;

import java.util.List;
import java.util.Optional;

/** Generic CRUD contract implemented by every JDBC DAO in the app. */
public interface GenericDao<T, ID> {
    T insert(T entity) throws PersistenceException;

    Optional<T> findById(ID id) throws PersistenceException;

    List<T> findAll() throws PersistenceException;

    void update(T entity) throws PersistenceException;

    void delete(ID id) throws PersistenceException;

    long count() throws PersistenceException;
}
