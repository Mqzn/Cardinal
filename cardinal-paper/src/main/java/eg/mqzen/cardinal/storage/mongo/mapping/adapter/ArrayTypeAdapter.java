package eg.mqzen.cardinal.storage.mongo.mapping.adapter;

import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.DeserializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.DeserializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.SerializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.SerializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.TypeAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayTypeAdapter implements TypeAdapter<Object[]> {
    @Override
    public boolean canHandle(TypeWrap<?> type) {
        return type.isArray();
    }

    /**
     * The type it handles mainly.
     *
     * @return the type it handles mainly.
     */
    @Override
    public TypeWrap<Object[]> type() {
        return new TypeWrap<>() {
        };
    }

    @Override
    public Object serialize(Object[] value, SerializationContext context) throws SerializationException {
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
    public Object[] deserialize(Object value, TypeWrap<Object[]> targetType, DeserializationContext context) throws DeserializationException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw new DeserializationException("Expected List for array deserialization");
        }
        
        TypeWrap<?> componentType = targetType.getComponentType();
        Object[] result = (Object[]) Array.newInstance(componentType.getRawType(), list.size());
        
        for (int i = 0; i < list.size(); i++) {
            result[i] = context.deserialize(list.get(i), componentType.getRawType());
        }
        
        return result;
    }
}