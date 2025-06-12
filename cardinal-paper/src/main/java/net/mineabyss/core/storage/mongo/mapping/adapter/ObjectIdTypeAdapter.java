package net.mineabyss.core.storage.mongo.mapping.adapter;

import com.mineabyss.lib.bson.types.ObjectId;
import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.core.storage.mongo.mapping.DeserializationContext;
import net.mineabyss.core.storage.mongo.mapping.SerializationContext;
import net.mineabyss.core.storage.mongo.mapping.TypeAdapter;

public class ObjectIdTypeAdapter implements TypeAdapter<ObjectId> {
    @Override
    public boolean canHandle(TypeWrap<?> type) {
        return ObjectId.class.isAssignableFrom(type.getRawType());
    }
    
    @Override
    public Object serialize(ObjectId value, SerializationContext context) {
        return value;
    }
    
    @Override
    public ObjectId deserialize(Object value, TypeWrap<ObjectId> targetType, DeserializationContext context) {
        return switch (value) {
            case null -> null;
            case ObjectId oid -> oid;
            case String str when ObjectId.isValid(str) -> new ObjectId(str);
            default -> throw new IllegalArgumentException("Cannot convert " + value + " to ObjectId");
        };
    }
}