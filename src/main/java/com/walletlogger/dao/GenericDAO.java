package com.walletlogger.dao;

import java.util.List;
import java.util.Optional;

import com.walletlogger.exceptions.TransactionPersistenceException;

/**
 * Generic DAO interface parameterised on:
 *   T  — the entity type (Transaction, AnomalyAlert, etc.)
 *   ID — the primary key type (String for our UUIDs)
 *
 * Every concrete DAO implements this, which means business logic
 * only ever talks to this interface and never touches JDBC directly.
 */
public interface GenericDAO<T, ID> {

    void insert(T entity) throws TransactionPersistenceException;

    Optional<T> findById(ID id) throws TransactionPersistenceException;

    List<T> findAll() throws TransactionPersistenceException;

    void update(T entity) throws TransactionPersistenceException;

    void delete(ID id) throws TransactionPersistenceException;
}
