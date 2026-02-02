package eg.mqzen.cardinal.storage.mongo;

import eg.mqzen.lib.bson.Document;
import eg.mqzen.lib.bson.types.ObjectId;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.lib.mongo.bulk.BulkWriteResult;
import eg.mqzen.lib.mongo.client.MongoCollection;
import eg.mqzen.lib.mongo.client.model.DeleteOneModel;
import eg.mqzen.lib.mongo.client.model.InsertOneModel;
import eg.mqzen.lib.mongo.client.model.ReplaceOneModel;
import eg.mqzen.lib.mongo.client.model.WriteModel;
import eg.mqzen.cardinal.api.storage.BatchOperation;
import eg.mqzen.cardinal.api.storage.BatchOperationResult;
import eg.mqzen.cardinal.api.storage.StorageEvent;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.api.storage.StorageMetrics;
import eg.mqzen.cardinal.api.storage.StorageObserver;
import eg.mqzen.cardinal.storage.mongo.mapping.DocumentMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of BatchOperation
 * 
 * @param <T> Entity type
 * @since 1.0
 */
public final class MongoBatchOperation<T> implements BatchOperation<T> {
    
    private final MongoCollection<Document> collection;
    private final DocumentMapper<T> mapper;
    private final List<StorageObserver> observers;
    private final StorageMetrics metrics;
    private final List<WriteModel<Document>> operations;
    private final List<String> errors;
    
    public MongoBatchOperation(MongoCollection<Document> collection, DocumentMapper<T> mapper,
                              List<StorageObserver> observers, StorageMetrics metrics) {
        this.collection = collection;
        this.mapper = mapper;
        this.observers = observers;
        this.metrics = metrics;
        this.operations = new ArrayList<>();
        this.errors = new ArrayList<>();
    }
    
    @Override
    public BatchOperation<T> insert(T entity) {
        try {
            Document doc = mapper.toDocument(entity);
            if (!doc.containsKey("_id")) {
                doc.put("_id", new ObjectId());
            }
            operations.add(new InsertOneModel<>(doc));
        } catch (Exception e) {
            errors.add("Insert failed: " + e.getMessage());
        }
        return this;
    }
    
    @Override
    public BatchOperation<T> update(T entity) {
        try {
            Document doc = mapper.toDocument(entity);
            if (doc.containsKey("_id")) {
                operations.add(new ReplaceOneModel<>(
                    new Document("_id", doc.get("_id")), 
                    doc
                ));
            } else {
                errors.add("Update failed: entity has no ID");
            }
        } catch (Exception e) {
            errors.add("Update failed: " + e.getMessage());
        }
        return this;
    }
    
    @Override
    public BatchOperation<T> delete(T entity) {
        try {
            Document doc = mapper.toDocument(entity);
            if (doc.containsKey("_id")) {
                operations.add(new DeleteOneModel<>(
                    new Document("_id", doc.get("_id"))
                ));
            } else {
                errors.add("Delete failed: entity has no ID");
            }
        } catch (Exception e) {
            errors.add("Delete failed: " + e.getMessage());
        }
        return this;
    }
    
    @Override
    public BatchOperationResult execute() throws StorageException {
        if (operations.isEmpty()) {
            return new BatchOperationResult(0, 0, 0, errors);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            BulkWriteResult result = collection.bulkWrite(operations);
            
            int insertCount = result.getInsertedCount();
            int updateCount = result.getModifiedCount();
            int deleteCount = result.getDeletedCount();
            
            metrics.recordOperation("batch", System.currentTimeMillis() - startTime);
            
            // Notify observers
            for (StorageObserver observer : observers) {
                try {
                    observer.onStorageEvent(new StorageEvent(
                        StorageEvent.Type.BATCH_OPERATION,
                        TypeWrap.of(Object.class),
                        result
                    ));
                } catch (Exception e) {
                    // Log but don't fail
                    System.err.println("Observer notification failed: " + e.getMessage());
                }
            }
            
            return new BatchOperationResult(insertCount, updateCount, deleteCount, errors);
            
        } catch (Exception e) {
            metrics.recordError("batch");
            throw new StorageException("Batch operation failed", e);
        }
    }
}