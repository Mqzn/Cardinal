package eg.mqzen.cardinal.storage.mysql;

import eg.mqzen.lib.commands.util.TypeUtility;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.lib.hikari.HikariConfig;
import eg.mqzen.lib.hikari.HikariDataSource;
import eg.mqzen.cardinal.api.storage.DBEntity;
import eg.mqzen.cardinal.api.storage.HealthStatus;
import eg.mqzen.cardinal.api.storage.QueryBuilder;
import eg.mqzen.cardinal.api.storage.Repository;
import eg.mqzen.cardinal.api.storage.StorageCommand;
import eg.mqzen.cardinal.api.storage.StorageConfig;
import eg.mqzen.cardinal.api.storage.StorageEngine;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.api.storage.StorageMetrics;
import eg.mqzen.cardinal.api.storage.StorageObserver;
import eg.mqzen.cardinal.api.storage.StorageType;
import eg.mqzen.cardinal.storage.MultiRepositoryQueryBuilder;
import eg.mqzen.cardinal.storage.StorageMetricsImpl;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.sql.DataSource;

/**
 * MySQL implementation of the StorageEngine interface.
 * Uses HikariCP for connection pooling and provides transaction support.
 */
public class MySQLStorageEngine implements StorageEngine {

    private final StorageConfig.MySQLConfig config;
    private final DataSource dataSource;
    private final ConcurrentHashMap<String, Repository<?, ?>> repositories;
    private final CopyOnWriteArrayList<StorageObserver> observers;
    private final StorageMetrics metrics;
    private final Executor asyncExecutor;
    private final MySQLEntityMetadata entityMetadata;
    private volatile boolean closed = false;
    
    public MySQLStorageEngine(StorageConfig.MySQLConfig config) throws StorageException {
        try {
            this.config = config;
            this.dataSource = createDataSource(config);
            this.repositories = new ConcurrentHashMap<>();
            this.observers = new CopyOnWriteArrayList<>();
            this.metrics = new StorageMetricsImpl();
            this.asyncExecutor = Executors.newFixedThreadPool(10);
            this.entityMetadata = new MySQLEntityMetadata();
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                // Connection test successful
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize MySQL storage engine", e);
        }
    }
    
    private DataSource createDataSource(StorageConfig.MySQLConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.url());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setConnectionTimeout(config.connectionTimeoutMs());
        hikariConfig.setMaxLifetime(config.maxLifetime());
        hikariConfig.setAutoCommit(config.autoCommit());
        
        // MySQL specific settings
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(hikariConfig);
    }

    @Override
    public Collection<? extends Repository<?, ?>> getRepositories() {
        return repositories.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID, T extends DBEntity<ID>> Repository<ID, T> getRepositoryOrCreate(String name, TypeWrap<T> entityClass) {
        if (closed) {
            throw new IllegalStateException("Storage engine is closed");
        }
        
        return (Repository<ID, T>) repositories.computeIfAbsent(name,
            clazz -> new MySQLRepository<>(name, entityClass, config, dataSource, entityMetadata, metrics, observers));
    }


    @Override
    public <ID, T extends DBEntity<ID>> QueryBuilder<T> queryAcrossRepositories(TypeWrap<T> entityClass) {
        // Get all repositories that store the specified entity class
        List<Repository<?, T>> matchingRepos =  repositories.values().stream()
                .filter(repo -> TypeUtility.areRelatedTypes(repo.getEntityType().getType(), entityClass.getType()))
                .map((r)-> (Repository<?, T>)r)
                .collect(Collectors.toList());

        return new MultiRepositoryQueryBuilder<>(matchingRepos, metrics);
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
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                T result = command.execute();
                conn.commit();
                metrics.recordOperation("transaction", System.currentTimeMillis() - startTime);
                return result;
            } catch (Exception e) {
                conn.rollback();
                metrics.recordError("transaction");
                throw new StorageException("Transaction failed", e);
            }
        } catch (SQLException e) {
            metrics.recordError("transaction");
            throw new StorageException("Failed to execute transaction", e);
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
        }, asyncExecutor);
    }
    
    @Override
    public HealthStatus getHealthStatus() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                return new HealthStatus(true, "MySQL connection is healthy", null);
            } else {
                return new HealthStatus(false, "MySQL connection is not valid", null);
            }
        } catch (SQLException e) {
            return new HealthStatus(false, "Failed to check MySQL connection", e.getMessage());
        }
    }
    
    @Override
    public StorageMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.MYSQL;
    }
    
    @Override
    public void close() throws Exception {
        closed = true;
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
        if (asyncExecutor instanceof AutoCloseable) {
            ((AutoCloseable) asyncExecutor).close();
        }
    }
}