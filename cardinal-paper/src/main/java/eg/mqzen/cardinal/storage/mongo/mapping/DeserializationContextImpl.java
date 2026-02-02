package eg.mqzen.cardinal.storage.mongo.mapping;


import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.DeserializationException;

class DeserializationContextImpl implements DeserializationContext {
    private final DocumentMapper<?> mapper;
    private final int depth;
    
    public DeserializationContextImpl(DocumentMapper<?> mapper) {
        this(mapper, 0);
    }
    
    private DeserializationContextImpl(DocumentMapper<?> mapper, int depth) {
        this.mapper = mapper;
        this.depth = depth;
    }
    
    @Override
    public <T> T deserialize(Object value, Class<T> targetType) throws DeserializationException {
        if (depth > 50) { // Prevent stack overflow
            throw new DeserializationException("Maximum deserialization depth exceeded");
        }
        
        DeserializationContextImpl childContext = new DeserializationContextImpl(mapper, depth + 1);
        return mapper.deserializeValue(value, TypeWrap.of(targetType), childContext);
    }
    
    @Override
    public int getDepth() {
        return depth;
    }
}