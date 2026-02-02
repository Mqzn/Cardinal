package eg.mqzen.cardinal.storage.mongo.mapping.adapter;

import eg.mqzen.lib.bson.Document;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.DeserializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.DeserializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.SerializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.SerializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.TypeAdapter;

import java.util.HashMap;
import java.util.Map;

public class MapTypeAdapter implements TypeAdapter<Map<?, ?>> {
    @Override
    public boolean canHandle(TypeWrap<?> type) {
        return Map.class.isAssignableFrom(type.getRawType());
    }
    
    @Override
    public Object serialize(Map<?, ?> value, SerializationContext context) throws SerializationException {
        if (value == null) {
            return null;
        }
        
        Document result = new Document();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            String key = entry.getKey().toString();
            Object serializedValue = context.serialize(entry.getValue());
            result.put(key, serializedValue);
        }
        return result;
    }
    
    @Override
    public Map<?, ?> deserialize(Object value, TypeWrap<Map<?, ?>> targetType, DeserializationContext context) throws DeserializationException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Document doc)) {
            throw new DeserializationException("Expected Document for Map deserialization");
        }
        
        Map<String, Object> result;
        if (targetType.getRawType().isInterface()) {
            result = new HashMap<>();
        } else {
            try {
                result = (Map<String, Object>) targetType.getRawType().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                result = new HashMap<>();
            }
        }
        
        result.putAll(doc);
        return result;
    }
}