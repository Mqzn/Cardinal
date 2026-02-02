package eg.mqzen.cardinal.storage.mongo.mapping;

import eg.mqzen.cardinal.storage.mongo.mapping.exception.DeserializationException;

/**
 * Context for deserialization operations
 */
public interface DeserializationContext {
    /**
     * Deserialize a nested object using the appropriate adapter or recursive processing
     */
    <T> T deserialize(Object value, Class<T> targetType) throws DeserializationException;
    
    /**
     * Get the current deserialization depth
     */
    int getDepth();
}