package eg.mqzen.cardinal.storage.mongo.mapping.adapter;


import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.DeserializationContext;

public class StringTypeAdapter extends PrimitiveTypeAdapter<String> {
    public StringTypeAdapter() {
        super(String.class);
    }
    
    @Override
    public String deserialize(Object value, TypeWrap<String> targetType, DeserializationContext context) {
        return value != null ? value.toString() : null;
    }
}