package net.mineabyss.core.storage.mongo.mapping.adapter;


import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.core.storage.mongo.mapping.DeserializationContext;

public class StringTypeAdapter extends PrimitiveTypeAdapter<String> {
    public StringTypeAdapter() {
        super(String.class);
    }
    
    @Override
    public String deserialize(Object value, TypeWrap<String> targetType, DeserializationContext context) {
        return value != null ? value.toString() : null;
    }
}