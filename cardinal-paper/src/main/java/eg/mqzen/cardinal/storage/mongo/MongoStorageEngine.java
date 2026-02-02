package eg.mqzen.cardinal.storage.mongo;

import eg.mqzen.cardinal.api.storage.QueryBuilder;
import eg.mqzen.cardinal.storage.MultiRepositoryQueryBuilder;
import eg.mqzen.lib.bson.Document;
import eg.mqzen.lib.commands.util.TypeUtility;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.lib.mongo.client.MongoClient;
import eg.mqzen.lib.mongo.client.MongoClients;
import eg.mqzen.lib.mongo.client.MongoDatabase;
import eg.mqzen.cardinal.api.storage.DBEntity;
import eg.mqzen.cardinal.api.storage.HealthStatus;
import eg.mqzen.cardinal.api.storage.Repository;
import eg.mqzen.cardinal.api.storage.StorageCommand;
import eg.mqzen.cardinal.api.storage.StorageConfig;
import eg.mqzen.cardinal.api.storage.StorageEngine;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.api.storage.StorageMetrics;
import eg.mqzen.cardinal.api.storage.StorageObserver;
import eg.mqzen.cardinal.api.storage.StorageType;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.storage.StorageMetricsImpl;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class MongoStorageEngine implements StorageEngine {
    
    private final MongoClient client;
    private final MongoDatabase database;
    private final StorageConfig.MongoConfig config;
    private final ConcurrentHashMap<String, Repository<?, ?>> repositories;
    private final List<StorageObserver> observers;
    private final StorageMetrics metrics;

    public MongoStorageEngine(StorageConfig.MongoConfig config) throws StorageException {
        this.config = config;
        this.repositories = new ConcurrentHashMap<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.metrics = new StorageMetricsImpl();

        Cardinal.log("Connecting to MongoDB database ...");
        Cardinal.log("ConnectionURI='%s', DB='%s'", config.uri(), config.database());
        try {
            this.client = MongoClients.create(config.uri());
            this.database = client.getDatabase(config.database());

            // Test connection
            database.runCommand(new Document("ping", 1));
        } catch (Exception e) {
            throw new StorageException("Failed to initialize MongoDB storage engine", e);
        }
    }

    private static void disableMongoLoggings() {
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");

        mongoLogger.setLevel(Level.OFF); // Turn off logging at source
        for (Handler handler : mongoLogger.getHandlers()) {
            mongoLogger.removeHandler(handler); // Remove all attached handlers
        }

        // If any parent handlers exist (e.g., root logger), prevent log bubbling
        mongoLogger.setUseParentHandlers(false);
    }

    @Override
    public Collection<? extends Repository<?, ?>> getRepositories() {
        return repositories.values();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ID, T extends DBEntity<ID>> Repository<ID, T> getRepositoryOrCreate(String name, TypeWrap<T> entityClass) {
        return (Repository<ID, T>) repositories.computeIfAbsent(name,
            k -> new MongoRepository<>(name, entityClass, database, config, observers, metrics));
    }
    
    @Override
    public void registerObserver(StorageObserver observer) {
        observers.add(observer);
    }
    
    @Override
    public void removeObserver(StorageObserver observer) {
        observers.remove(observer);
    }
    
    @Override
    public <T> T executeTransaction(StorageCommand<T> command) throws StorageException {
        // MongoDB transactions require replica set or sharded cluster
        // For single node, we'll execute directly
        try {
            return command.execute();
        } catch (Exception e) {
            throw new StorageException("Transaction failed", e);
        }
    }
    
    @Override
    public <T> CompletableFuture<T> executeTransactionAsync(StorageCommand<T> command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeTransaction(command);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public HealthStatus getHealthStatus() {
        try {
            database.runCommand(new Document("ping", 1));
            return new HealthStatus(true, "MongoDB connection healthy", null);
        } catch (Exception e) {
            return new HealthStatus(false, "MongoDB connection failed", e.getMessage());
        }
    }
    
    @Override
    public StorageMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.MONGO;
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public <ID, T extends DBEntity<ID>> QueryBuilder<T> queryAcrossRepositories(TypeWrap<T> entityClass) {
        List<Repository<?, T>> matchingRepos =  repositories.values().stream()
                .filter(repo -> TypeUtility.areRelatedTypes(repo.getEntityType().getType(), entityClass.getType()))
                .map((r)-> (Repository<?, T>)r)
                .collect(Collectors.toList());

        return new MultiRepositoryQueryBuilder<>(matchingRepos, metrics);
    }
}