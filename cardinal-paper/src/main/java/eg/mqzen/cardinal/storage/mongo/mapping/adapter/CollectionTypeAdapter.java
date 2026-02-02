package eg.mqzen.cardinal.storage.mongo.mapping.adapter;

import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.DeserializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.DeserializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.SerializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.SerializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.TypeAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionTypeAdapter implements TypeAdapter<Collection<?>> {
    @Override
    public boolean canHandle(TypeWrap<?> type) {
        return Collection.class.isAssignableFrom(type.getRawType());
    }


    @Override
    public Object serialize(Collection<?> value, SerializationContext context) throws SerializationException {
        if (value == null) {
            return null;
        }
        
        List<Object> result = new ArrayList<>();
        for (Object item : value) {
            result.add(context.serialize(item));
        }
        return result;
    }
    
    @Override
    public Collection<?> deserialize(Object value, TypeWrap<Collection<?>> targetType, DeserializationContext context) throws DeserializationException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw new DeserializationException("Expected List for Collection deserialization");
        }
        
        Collection<Object> result;
        if (targetType.getRawType().isInterface()) {
            if (targetType.isSubtypeOf(Set.class)) {
                result = new HashSet<>();
            } else {
                result = new ArrayList<>();
            }
        } else {
            try {
                result = (Collection<Object>) targetType.getRawType().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                result = new ArrayList<>();
            }
        }
        
        // Note: Generic type information is lost at runtime, so we store items as-is
        // In a real implementation, you might want to use TypeToken or similar for generic preservation
        result.addAll(list);
        return result;
    }
}