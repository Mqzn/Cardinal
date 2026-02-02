package eg.mqzen.cardinal.storage.mongo.mapping;

import eg.mqzen.cardinal.storage.mongo.mapping.exception.SerializationException;

/**
 * Context for serialization operations
 */
public interface SerializationContext {
    /**
     * Serialize a nested object using the appropriate adapter or recursive processing
     */
    Object serialize(Object value) throws SerializationException;
    
    /**
     * Get the current serialization depth (for cycle detection)
     */
    int getDepth();
    
    /**
     * Check if an object is currently being serialized (cycle detection)
     */
    boolean isSerializing(Object obj);
}