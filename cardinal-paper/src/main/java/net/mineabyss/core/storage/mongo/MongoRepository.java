package net.mineabyss.core.storage.mongo;

import com.mineabyss.lib.bson.Document;
import com.mineabyss.lib.commands.util.TypeWrap;
import com.mineabyss.lib.mongo.client.MongoCollection;
import com.mineabyss.lib.mongo.client.MongoCursor;
import com.mineabyss.lib.mongo.client.MongoDatabase;
import com.mineabyss.lib.mongo.client.model.Filters;
import com.mineabyss.lib.mongo.client.model.ReplaceOptions;
import net.mineabyss.cardinal.api.storage.BatchOperation;
import net.mineabyss.cardinal.api.storage.DBEntity;
import net.mineabyss.cardinal.api.storage.QueryBuilder;
import net.mineabyss.cardinal.api.storage.Repository;
import net.mineabyss.cardinal.api.storage.StorageConfig;
import net.mineabyss.cardinal.api.storage.StorageEvent;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.storage.StorageMetrics;
import net.mineabyss.cardinal.api.storage.StorageObserver;
import net.mineabyss.core.storage.mongo.mapping.DocumentMapper;
import net.mineabyss.core.storage.mongo.mapping.exception.SerializationException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MongoRepository<ID, T extends DBEntity<ID>> implements Repository<ID, T> {

    private final String name;
    private final TypeWrap<T> entityType;
    private final MongoCollection<Document> collection;
    private final List<StorageObserver> observers;
    private final StorageMetrics metrics;
    private final DocumentMapper<T> mapper;
    
    public MongoRepository(
            String name,
            TypeWrap<T> entityType,
            MongoDatabase database,
            StorageConfig.MongoConfig config,
            List<StorageObserver> observers,
            StorageMetrics metrics
    ) {
        this.name = name;
        this.entityType = entityType;
        this.observers = observers;
        this.metrics = metrics;
        this.mapper = new DocumentMapper<>(entityType);
        
        String collectionName = config.collectionPrefix() + name;
        boolean collectionExists = false;
        try(MongoCursor<String> listNames = database.listCollectionNames().cursor()) {
            while (listNames.hasNext()) {
                String nextName = listNames.next();
                if(nextName.equals(collectionName)) {
                    collectionExists = true;
                    break;
                }
            }
        }

        if(!collectionExists) {
            database.createCollection(collectionName);
        }
        this.collection = database.getCollection(collectionName);
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
        try {
            long startTime = System.currentTimeMillis();
            Document doc = mapper.toDocument(entity);
            collection.replaceOne(Filters.eq("id", entity.getEntityID().toString()),doc, new ReplaceOptions().upsert(true));

            metrics.recordOperation("save", System.currentTimeMillis() - startTime);
            notifyObservers(new StorageEvent(StorageEvent.Type.ENTITY_SAVED, entityType, entity));
            
            return entity;
        } catch (Exception e) {
            metrics.recordError("save");
            throw new StorageException("Failed to save entity", e);
        }
    }
    
    @Override
    public List<T> saveAll(List<T> entities) throws StorageException {
        List<T> savedEntities = new ArrayList<>();
        for (T entity : entities) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }
    
    @Override
    public Optional<T> findById(ID id) throws StorageException {
        try {
            long startTime = System.currentTimeMillis();
            Document doc = collection.find(new Document("id", id.toString())).first();
            
            metrics.recordOperation("findById", System.currentTimeMillis() - startTime);
            
            return doc != null ? Optional.of(mapper.fromDocument(doc)) : Optional.empty();
        } catch (Exception e) {
            metrics.recordError("findById");
            throw new StorageException("Failed to find entity by id", e);
        }
    }
    
    @Override
    public List<T> findAll() throws StorageException {
        try {
            long startTime = System.currentTimeMillis();
            List<T> entities = new ArrayList<>();
            
            for (Document doc : collection.find()) {
                entities.add(mapper.fromDocument(doc));
            }
            
            metrics.recordOperation("findAll", System.currentTimeMillis() - startTime);
            return entities;
        } catch (Exception e) {
            metrics.recordError("findAll");
            throw new StorageException("Failed to find all entities", e);
        }
    }
    
    @Override
    public void deleteById(ID id) throws StorageException {
        try {
            long startTime = System.currentTimeMillis();
            collection.deleteOne(new Document("_id", id.toString()));
            
            metrics.recordOperation("deleteById", System.currentTimeMillis() - startTime);
            notifyObservers(new StorageEvent(StorageEvent.Type.ENTITY_DELETED, entityType, id));
        } catch (Exception e) {
            metrics.recordError("deleteById");
            throw new StorageException("Failed to delete entity by id", e);
        }
    }
    
    @Override
    public void delete(T entity) throws StorageException {
        Document doc;
        try {
            doc = mapper.toDocument(entity);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        if (doc.containsKey("_id")) {
            deleteById((ID) doc.get("_id"));
        }
    }
    
    @Override
    public void deleteAll(List<T> entities) throws StorageException {
        for (T entity : entities) {
            delete(entity);
        }
    }
    
    @Override
    public boolean existsById(ID id) throws StorageException {
        try {
            return collection.countDocuments(new Document("id", id.toString())) > 0;
        } catch (Exception e) {
            throw new StorageException("Failed to check entity existence", e);
        }
    }
    
    @Override
    public long count() throws StorageException {
        try {
            return collection.countDocuments();
        } catch (Exception e) {
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
        return new MongoQueryBuilder<>(collection, mapper, metrics);
    }
    
    @Override
    public BatchOperation<T> batch() {
        return new MongoBatchOperation<>(collection, mapper, observers, metrics);
    }
    
    @Override
    public TypeWrap<T> getEntityType() {
        return entityType;
    }
    
    private void notifyObservers(StorageEvent event) {
        for (StorageObserver observer : observers) {
            try {
                observer.onStorageEvent(event);
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Observer notification failed: " + e.getMessage());
            }
        }
    }
}