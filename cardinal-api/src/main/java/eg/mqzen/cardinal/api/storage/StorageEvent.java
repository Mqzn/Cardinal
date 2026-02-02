package eg.mqzen.cardinal.api.storage;

import eg.mqzen.lib.commands.util.TypeWrap;

/**
 * Storage event record
 * 
 * @since 1.0
 */
public record StorageEvent(
    Type type,
    TypeWrap<?> entityClass,
    Object data,
    long timestamp
) {
    public StorageEvent(Type type, TypeWrap<?> entityClass, Object data) {
        this(type, entityClass, data, System.currentTimeMillis());
    }
    
    public enum Type {
        ENTITY_SAVED, ENTITY_UPDATED, ENTITY_DELETED, BATCH_OPERATION
    }
}