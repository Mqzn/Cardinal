package net.mineabyss.cardinal.api.storage;

/**
 * Batch operation interface
 * 
 * @since 1.0
 */
public interface BatchOperation<T> {
    BatchOperation<T> insert(T entity);
    BatchOperation<T> update(T entity);
    BatchOperation<T> delete(T entity);
    BatchOperationResult execute() throws StorageException;
}