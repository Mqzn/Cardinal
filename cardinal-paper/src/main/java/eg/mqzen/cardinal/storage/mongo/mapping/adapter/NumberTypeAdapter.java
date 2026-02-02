package eg.mqzen.cardinal.storage.mongo.mapping.adapter;

import eg.mqzen.lib.commands.util.TypeUtility;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.DeserializationContext;

import java.lang.reflect.Type;

public class NumberTypeAdapter extends PrimitiveTypeAdapter<Number> {
    public NumberTypeAdapter() {
        super(Integer.class, int.class, Long.class, long.class, 
              Double.class, double.class, Float.class, float.class,
              Short.class, short.class, Byte.class, byte.class);
    }
    
    @Override
    public Number deserialize(Object value, TypeWrap<Number> typeWrap, DeserializationContext context) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Expected Number, got " + value.getClass());
        }
        Type targetType = typeWrap.getType();

        if (TypeUtility.matches(targetType, int.class)) return number.intValue();
        if (TypeUtility.matches(targetType, long.class)) return number.longValue();
        if (TypeUtility.matches(targetType, double.class)) return number.doubleValue();
        if (TypeUtility.matches(targetType, float.class)) return number.floatValue();
        if (TypeUtility.matches(targetType, short.class)) return number.shortValue();
        if (TypeUtility.matches(targetType, byte.class)) return number.byteValue();
        return number;
    }
}