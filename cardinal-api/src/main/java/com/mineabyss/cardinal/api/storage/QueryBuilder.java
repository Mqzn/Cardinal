package com.mineabyss.cardinal.api.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent query builder interface
 */
public interface QueryBuilder<T> {
    
    QueryBuilder<T> where(String field);
    QueryBuilder<T> eq(Object value);
    QueryBuilder<T> ne(Object value);
    QueryBuilder<T> gt(Object value);
    QueryBuilder<T> gte(Object value);
    QueryBuilder<T> lt(Object value);
    QueryBuilder<T> lte(Object value);
    QueryBuilder<T> in(List<Object> values);
    QueryBuilder<T> like(String pattern);
    QueryBuilder<T> and();
    QueryBuilder<T> or();
    QueryBuilder<T> not();
    
    QueryBuilder<T> sortBy(Class<?> sortEntityTypeClass, String field, SortOrder order);
    QueryBuilder<T> limit(int limit);
    QueryBuilder<T> skip(int skip);
    
    List<T> execute() throws StorageException;
    CompletableFuture<List<T>> executeAsync();
    Optional<T> findFirst() throws StorageException;
    long count() throws StorageException;
    
    enum SortOrder {
        ASC, DESC
    }
}