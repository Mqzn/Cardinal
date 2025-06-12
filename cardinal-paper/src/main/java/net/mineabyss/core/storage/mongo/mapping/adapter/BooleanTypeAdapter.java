package net.mineabyss.core.storage.mongo.mapping.adapter;


import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.core.storage.mongo.mapping.DeserializationContext;

public class BooleanTypeAdapter extends PrimitiveTypeAdapter<Boolean> {
    public BooleanTypeAdapter() {
        super(Boolean.class, boolean.class);
    }
    
    @Override
    public Boolean deserialize(Object value, TypeWrap<Boolean> targetType, DeserializationContext context) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }

        return Boolean.parseBoolean(value.toString());
    }
}