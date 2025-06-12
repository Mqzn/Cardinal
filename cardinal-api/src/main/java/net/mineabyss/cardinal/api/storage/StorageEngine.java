package net.mineabyss.cardinal.api.storage;

import com.mineabyss.lib.commands.util.TypeWrap;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Main facade interface for the storage engine providing unified access to different database backends.
 * Implements the Facade pattern to simplify complex subsystem interactions.
 */
public interface StorageEngine extends AutoCloseable {

    Collection<? extends Repository<?, ?>> getRepositories();
    /**
     * Get a repository for the specified name and entity class
     */
    <ID, T extends DBEntity<ID>> Repository<ID, T> getRepositoryOrCreate(String name, TypeWrap<T> entityType);

    /**
     * Register an observer for storage events
     */
    void registerObserver(StorageObserver observer);
    
    /**
     * Remove an observer
     */
    void removeObserver(StorageObserver observer);
    
    /**
     * Execute a command within a transaction
     */
    <T> T executeTransaction(StorageCommand<T> command) throws StorageException;
    
    /**
     * Execute a command within a transaction asynchronously
     */
    <T> CompletableFuture<T> executeTransactionAsync(StorageCommand<T> command);
    
    /**
     * Get health status of the storage engine
     */
    HealthStatus getHealthStatus();
    
    /**
     * Get storage metrics
     */
    StorageMetrics getMetrics();
    
    /**
     * Get the storage type
     */
    StorageType getStorageType();

    /**
     * Creates a query builder that can query across multiple repositories
     * containing the same entity type.
     *
     * @param entityClass the entity class to query for
     * @return a query builder that can search across multiple repositories
     */
    default <ID, T extends DBEntity<ID>> QueryBuilder<T> queryAcrossRepositories(TypeWrap<T> entityClass) {
        throw new UnsupportedOperationException("Cross-repository queries are not supported by this storage engine");
    }}