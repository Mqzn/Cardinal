package eg.mqzen.cardinal.storage.mongo.mapping;

import eg.mqzen.cardinal.storage.mongo.mapping.exception.SerializationException;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

class SerializationContextImpl implements SerializationContext {
    private final DocumentMapper<?> mapper;
    private final Set<Object> serializingObjects;
    private final int depth;
    
    public SerializationContextImpl(DocumentMapper<?> mapper) {
        this(mapper, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }
    
    private SerializationContextImpl(DocumentMapper<?> mapper, Set<Object> serializingObjects, int depth) {
        this.mapper = mapper;
        this.serializingObjects = serializingObjects;
        this.depth = depth;
    }
    
    @Override
    public Object serialize(Object value) throws SerializationException {
        if (depth > 50) { // Prevent stack overflow
            throw new SerializationException("Maximum serialization depth exceeded");
        }
        
        SerializationContextImpl childContext = new SerializationContextImpl(mapper, serializingObjects, depth + 1);
        return mapper.serializeValue(value, childContext);
    }
    
    @Override
    public int getDepth() {
        return depth;
    }
    
    @Override
    public boolean isSerializing(Object obj) {
        return serializingObjects.contains(obj);
    }
    
    public void markSerializing(Object obj) {
        serializingObjects.add(obj);
    }
    
    public void unmarkSerializing(Object obj) {
        serializingObjects.remove(obj);
    }
}
