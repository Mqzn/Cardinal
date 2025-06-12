package net.mineabyss.core.storage.mongo.mapping;

import net.mineabyss.core.storage.mongo.mapping.exception.DeserializationException;

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