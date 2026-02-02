package eg.mqzen.cardinal.storage.mongo.mapping.adapter;

import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.SerializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.TypeAdapter;

import java.util.Set;

/**
 * Abstract base for primitive and wrapper type adapters
 */
public abstract class PrimitiveTypeAdapter<T> implements TypeAdapter<T> {
    private final Set<Class<?>> supportedTypes;
    
    protected PrimitiveTypeAdapter(Class<?>... supportedTypes) {
        this.supportedTypes = Set.of(supportedTypes);
    }
    
    @Override
    public boolean canHandle(TypeWrap<?> type) {
        return supportedTypes.contains(type.getRawType());
    }
    
    @Override
    public Object serialize(T value, SerializationContext context) {
        return value; // Primitives are stored as-is in MongoDB
    }
}