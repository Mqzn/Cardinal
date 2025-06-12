package net.mineabyss.core.storage.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.cardinal.api.storage.BatchOperation;
import net.mineabyss.cardinal.api.storage.DBEntity;
import net.mineabyss.cardinal.api.storage.QueryBuilder;
import net.mineabyss.cardinal.api.storage.Repository;
import net.mineabyss.cardinal.api.storage.StorageConfig;
import net.mineabyss.cardinal.api.storage.StorageEvent;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.storage.StorageMetrics;
import net.mineabyss.cardinal.api.storage.StorageObserver;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

/**
 * MySQL implementation of the Repository interface.
 * Handles entity persistence using reflection and JSON serialization for complex types.
 */
public class MySQLRepository<ID, T extends DBEntity<ID>> implements Repository<ID, T> {

    private final String name;
    private final TypeWrap<T> entityClass;
    private final DataSource dataSource;
    private final MySQLEntityMetadata entityMetadata;
    private final StorageMetrics metrics;
    private final CopyOnWriteArrayList<StorageObserver> observers;
    private final ObjectMapper objectMapper;
    private final String tableName;
    
    public MySQLRepository(
            String name,
            TypeWrap<T> entityClass,
            StorageConfig.MySQLConfig config,
            DataSource dataSource,
            MySQLEntityMetadata entityMetadata,
            StorageMetrics metrics,
            CopyOnWriteArrayList<StorageObserver> observers
    ) {
        this.name = name;
        this.entityClass = entityClass;
        this.dataSource = dataSource;
        this.entityMetadata = entityMetadata;
        this.metrics = metrics;
        this.observers = observers;
        this.objectMapper = new ObjectMapper();
        this.tableName = config.tablePrefix() + name;
        
        // Ensure table exists
        createTableIfNotExists();
    }
    
    private void createTableIfNotExists() {
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(255) PRIMARY KEY,
                    data JSON NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """.formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create table: " + tableName, e);
        }
    }

    /**
     * Represents a unique name for the repo.
     *
     * @return the unique name for the repo.
     */
    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public T save(T entity) throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            ID id = extractId(entity);
            String json = objectMapper.writeValueAsString(entity);
            
            String sql = """
                INSERT INTO %s (id, data) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = ?, updated_at = CURRENT_TIMESTAMP
                """.formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                stmt.setString(2, json);
                stmt.setString(3, json);
                stmt.executeUpdate();
            }
            
            metrics.recordOperation("save", System.currentTimeMillis() - startTime);
            notifyObservers(new StorageEvent(StorageEvent.Type.ENTITY_SAVED, entityClass, entity));
            return entity;
            
        } catch (Exception e) {
            metrics.recordError("save");
            throw new StorageException("Failed to save entity", e);
        }
    }
    
    @Override
    public List<T> saveAll(List<T> entities) throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                INSERT INTO %s (id, data) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = ?, updated_at = CURRENT_TIMESTAMP
                """.formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (T entity : entities) {
                    ID id = extractId(entity);
                    String json = objectMapper.writeValueAsString(entity);
                    
                    stmt.setString(1, id.toString());
                    stmt.setString(2, json);
                    stmt.setString(3, json);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            metrics.recordOperation("saveAll", System.currentTimeMillis() - startTime);
            notifyObservers(new StorageEvent(StorageEvent.Type.BATCH_OPERATION, entityClass, entities));
            return entities;
            
        } catch (Exception e) {
            metrics.recordError("saveAll");
            throw new StorageException("Failed to save entities", e);
        }
    }
    
    @Override
    public Optional<T> findById(ID id) throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT data FROM %s WHERE id = ?".formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String json = rs.getString("data");
                        T entity = (T) objectMapper.readValue(json, entityClass.getRawType());
                        metrics.recordOperation("findById", System.currentTimeMillis() - startTime);
                        return Optional.of(entity);
                    }
                }
            }
            
            metrics.recordOperation("findById", System.currentTimeMillis() - startTime);
            return Optional.empty();
            
        } catch (Exception e) {
            metrics.recordError("findById");
            throw new StorageException("Failed to find entity by id", e);
        }
    }
    
    @Override
    public List<T> findAll() throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT data FROM %s".formatted(tableName);
            List<T> results = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String json = rs.getString("data");
                    T entity = (T) objectMapper.readValue(json, entityClass.getRawType());
                    results.add(entity);
                }
            }
            
            metrics.recordOperation("findAll", System.currentTimeMillis() - startTime);
            return results;
            
        } catch (Exception e) {
            metrics.recordError("findAll");
            throw new StorageException("Failed to find all entities", e);
        }
    }
    
    @Override
    public void deleteById(ID id) throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "DELETE FROM %s WHERE id = ?".formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                stmt.executeUpdate();
            }
            
            metrics.recordOperation("deleteById", System.currentTimeMillis() - startTime);
            notifyObservers(new StorageEvent(StorageEvent.Type.ENTITY_DELETED, entityClass, id));
            
        } catch (SQLException e) {
            metrics.recordError("deleteById");
            throw new StorageException("Failed to delete entity by id", e);
        }
    }
    
    @Override
    public void delete(T entity) throws StorageException {
        ID id = extractId(entity);
        deleteById(id);
    }
    
    @Override
    public void deleteAll(List<T> entities) throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "DELETE FROM %s WHERE id = ?".formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (T entity : entities) {
                    ID id = extractId(entity);
                    stmt.setString(1, id.toString());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            metrics.recordOperation("deleteAll", System.currentTimeMillis() - startTime);
            notifyObservers(new StorageEvent(StorageEvent.Type.BATCH_OPERATION, entityClass, entities));
            
        } catch (Exception e) {
            metrics.recordError("deleteAll");
            throw new StorageException("Failed to delete entities", e);
        }
    }
    
    @Override
    public boolean existsById(ID id) throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT 1 FROM %s WHERE id = ? LIMIT 1".formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean exists = rs.next();
                    metrics.recordOperation("existsById", System.currentTimeMillis() - startTime);
                    return exists;
                }
            }
            
        } catch (SQLException e) {
            metrics.recordError("existsById");
            throw new StorageException("Failed to check entity existence", e);
        }
    }
    
    @Override
    public long count() throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) FROM %s".formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    long count = rs.getLong(1);
                    metrics.recordOperation("count", System.currentTimeMillis() - startTime);
                    return count;
                }
                
                return 0;
            }
            
        } catch (SQLException e) {
            metrics.recordError("count");
            throw new StorageException("Failed to count entities", e);
        }
    }
    
    @Override
    public CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return save(entity);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findById(id);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<T>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findAll();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public QueryBuilder<T> query() {
        return new MySQLQueryBuilder<>(entityClass, dataSource, tableName, objectMapper, metrics);
    }
    
    @Override
    public BatchOperation<T> batch() {
        return new MySQLBatchOperation<>(entityClass, dataSource, tableName, objectMapper, metrics, observers);
    }
    
    @Override
    public TypeWrap<T> getEntityType() {
        return entityClass;
    }
    
    @SuppressWarnings("unchecked")
    private ID extractId(T entity) {
        try {
            Field idField = findIdField(entityClass.getRawType());
            idField.setAccessible(true);
            return (ID) idField.get(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract ID from entity", e);
        }
    }
    
    private Field findIdField(Class<?> clazz) {
        // Look for field named "id" first
        try {
            return clazz.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            // Look for any field annotated with @Id (if using JPA annotations)
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().toLowerCase().contains("id")) {
                    return field;
                }
            }
            throw new RuntimeException("No ID field found in entity class: " + clazz.getName());
        }
    }
    
    private void notifyObservers(StorageEvent event) {
        for (StorageObserver observer : observers) {
            try {
                observer.onStorageEvent(event);
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error notifying observer: " + e.getMessage());
            }
        }
    }
}