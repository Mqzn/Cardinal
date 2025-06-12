package net.mineabyss.core.storage.mongo.mapping;

import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.core.storage.mongo.mapping.exception.DeserializationException;
import net.mineabyss.core.storage.mongo.mapping.exception.SerializationException;

/**
 * Base interface for type adapters that handle serialization/deserialization
 * @param <T> The type this adapter handles
 */
public interface TypeAdapter<T> {

    /**
     * The type it handles mainly.
     * @return the type it handles mainly.
     */
    default TypeWrap<T> type() {
        return new TypeWrap<>() {};
    }
    /**
     * Serialize an object to a MongoDB-compatible value
     */
    Object serialize(T value, SerializationContext context) throws SerializationException;
    
    /**
     * Deserialize a MongoDB value to an object
     */
    T deserialize(Object value, TypeWrap<T> targetType, DeserializationContext context) throws DeserializationException;
    
    /**
     * Check if this adapter can handle the given type
     */
    boolean canHandle(TypeWrap<?> type);
}