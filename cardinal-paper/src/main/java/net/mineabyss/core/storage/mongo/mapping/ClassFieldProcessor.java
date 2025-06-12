package net.mineabyss.core.storage.mongo.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles field access for regular classes
 */
class ClassFieldProcessor {
    private final Map<String, Field> fieldMap;
    
    public ClassFieldProcessor(Class<?> clazz) {
        this.fieldMap = new HashMap<>();
        cacheFields(clazz);
    }
    
    private void cacheFields(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) &&
                    !Modifier.isTransient(field.getModifiers())) {
                    field.setAccessible(true);
                    fieldMap.putIfAbsent(field.getName(), field);
                }
            }
            current = current.getSuperclass();
        }
    }
    
    public Set<String> getFieldNames() {
        return fieldMap.keySet();
    }
    
    public Object getFieldValue(Object instance, String fieldName) throws IllegalAccessException {
        Field field = fieldMap.get(fieldName);
        if(field != null && field.getDeclaredAnnotation(ExcludeField.class) != null ) {
            return null;
        }
        return field != null ? field.get(instance) : null;
    }
    
    public void setFieldValue(Object instance, String fieldName, Object value) throws IllegalAccessException {
        Field field = fieldMap.get(fieldName);
        if (field != null) {
            field.set(instance, value);
        }
    }
    
    public Class<?> getFieldType(String fieldName) {
        Field field = fieldMap.get(fieldName);
        return field != null ? field.getType() : null;
    }
}