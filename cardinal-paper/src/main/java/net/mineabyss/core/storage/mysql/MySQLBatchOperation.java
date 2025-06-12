package net.mineabyss.core.storage.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.cardinal.api.storage.BatchOperation;
import net.mineabyss.cardinal.api.storage.BatchOperationResult;
import net.mineabyss.cardinal.api.storage.StorageEvent;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.storage.StorageMetrics;
import net.mineabyss.cardinal.api.storage.StorageObserver;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

/**
 * MySQL implementation of batch operations for efficient bulk operations.
 */
public class MySQLBatchOperation<T> implements BatchOperation<T> {
    
    private final TypeWrap<T> entityClass;
    private final DataSource dataSource;
    private final String tableName;
    private final ObjectMapper objectMapper;
    private final StorageMetrics metrics;
    private final CopyOnWriteArrayList<StorageObserver> observers;
    
    private final List<T> toInsert = new ArrayList<>();
    private final List<T> toUpdate = new ArrayList<>();
    private final List<T> toDelete = new ArrayList<>();
    
    public MySQLBatchOperation(TypeWrap<T> entityClass, DataSource dataSource, String tableName,
                              ObjectMapper objectMapper, StorageMetrics metrics,
                              CopyOnWriteArrayList<StorageObserver> observers) {
        this.entityClass = entityClass;
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.observers = observers;
    }
    
    @Override
    public BatchOperation<T> insert(T entity) {
        toInsert.add(entity);
        return this;
    }
    
    @Override
    public BatchOperation<T> update(T entity) {
        toUpdate.add(entity);
        return this;
    }
    
    @Override
    public BatchOperation<T> delete(T entity) {
        toDelete.add(entity);
        return this;
    }
    
    @Override
    public BatchOperationResult execute() throws StorageException {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        int insertedCount = 0;
        int updatedCount = 0;
        int deletedCount = 0;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Execute inserts
                if (!toInsert.isEmpty()) {
                    insertedCount = executeInserts(conn, errors);
                }
                
                // Execute updates
                if (!toUpdate.isEmpty()) {
                    updatedCount = executeUpdates(conn, errors);
                }
                
                // Execute deletes
                if (!toDelete.isEmpty()) {
                    deletedCount = executeDeletes(conn, errors);
                }
                
                conn.commit();
                metrics.recordOperation("batch", System.currentTimeMillis() - startTime);
                
                // Notify observers
                if (insertedCount > 0 || updatedCount > 0 || deletedCount > 0) {
                    List<T> allEntities = new ArrayList<>();
                    allEntities.addAll(toInsert);
                    allEntities.addAll(toUpdate);
                    allEntities.addAll(toDelete);
                    
                    notifyObservers(new StorageEvent(StorageEvent.Type.BATCH_OPERATION, entityClass, allEntities));
                }
                
                return new BatchOperationResult(insertedCount, updatedCount, deletedCount, errors);
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            
        } catch (Exception e) {
            metrics.recordError("batch");
            throw new StorageException("Failed to execute batch operation", e);
        }
    }
    
    private int executeInserts(Connection conn, List<String> errors) throws Exception {
        String sql = """
            INSERT INTO %s (id, data) VALUES (?, ?)
            """.formatted(tableName);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int count = 0;
            for (T entity : toInsert) {
                try {
                    Object id = extractId(entity);
                    String json = objectMapper.writeValueAsString(entity);
                    
                    stmt.setString(1, id.toString());
                    stmt.setString(2, json);
                    stmt.addBatch();
                    count++;
                } catch (Exception e) {
                    errors.add("Insert failed for entity: " + e.getMessage());
                }
            }
            
            if (count > 0) {
                stmt.executeBatch();
            }
            return count;
        }
    }
    
    private int executeUpdates(Connection conn, List<String> errors) throws Exception {
        String sql = """
            UPDATE %s SET data = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
            """.formatted(tableName);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int count = 0;
            for (T entity : toUpdate) {
                try {
                    Object id = extractId(entity);
                    String json = objectMapper.writeValueAsString(entity);
                    
                    stmt.setString(1, json);
                    stmt.setString(2, id.toString());
                    stmt.addBatch();
                    count++;
                } catch (Exception e) {
                    errors.add("Update failed for entity: " + e.getMessage());
                }
            }
            
            if (count > 0) {
                stmt.executeBatch();
            }
            return count;
        }
    }
    
    private int executeDeletes(Connection conn, List<String> errors) throws Exception {
        String sql = "DELETE FROM %s WHERE id = ?".formatted(tableName);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int count = 0;
            for (T entity : toDelete) {
                try {
                    Object id = extractId(entity);
                    stmt.setString(1, id.toString());
                    stmt.addBatch();
                    count++;
                } catch (Exception e) {
                    errors.add("Delete failed for entity: " + e.getMessage());
                }
            }
            
            if (count > 0) {
                stmt.executeBatch();
            }
            return count;
        }
    }
    
    private Object extractId(T entity) throws Exception {
        Field idField = findIdField(entityClass.getRawType());
        idField.setAccessible(true);
        return idField.get(entity);
    }
    
    private Field findIdField(Class<?> clazz) {
        try {
            return clazz.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
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
                System.err.println("Error notifying observer: " + e.getMessage());
            }
        }
    }
}