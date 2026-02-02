package eg.mqzen.cardinal.api.storage;

import eg.mqzen.lib.commands.util.TypeWrap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generic repository interface providing CRUD operations and query capabilities
 */
public interface Repository<ID, T extends DBEntity<ID>> {

    /**
     * Represents a unique name for the repo.
     * @return the unique name for the repo.
     */
    @NotNull String getName();

    // Basic CRUD operations
    T save(T entity) throws StorageException;
    List<T> saveAll(List<T> entities) throws StorageException;
    Optional<T> findById(ID id) throws StorageException;
    List<T> findAll() throws StorageException;
    void deleteById(ID id) throws StorageException;
    void delete(T entity) throws StorageException;
    void deleteAll(List<T> entities) throws StorageException;
    boolean existsById(ID id) throws StorageException;
    long count() throws StorageException;
    
    // Async operations
    CompletableFuture<T> saveAsync(T entity);
    CompletableFuture<Optional<T>> findByIdAsync(ID id);
    CompletableFuture<List<T>> findAllAsync();
    
    // Query builder
    QueryBuilder<T> query();
    
    // Batch operations
    BatchOperation<T> batch();
    
    // Entity class
    TypeWrap<T> getEntityType();
}