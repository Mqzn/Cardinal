package net.mineabyss.core.storage.mongo.mapping;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles field access for record classes
 */
class RecordFieldProcessor {
    private final RecordComponent[] components;
    private final Map<String, RecordComponent> componentMap;
    
    public RecordFieldProcessor(Class<?> recordClass) {
        this.components = recordClass.getRecordComponents();
        this.componentMap = new HashMap<>();
        for (RecordComponent component : components) {
            componentMap.put(component.getName(), component);
        }
    }
    
    public Set<String> getFieldNames() {
        return componentMap.keySet();
    }
    
    public Object getFieldValue(Object instance, String fieldName) throws Exception {
        RecordComponent component = componentMap.get(fieldName);
        return component != null ? component.getAccessor().invoke(instance) : null;
    }
    
    public Class<?> getFieldType(String fieldName) {
        RecordComponent component = componentMap.get(fieldName);
        return component != null ? component.getType() : null;
    }
    
    public Object createInstance(Map<String, Object> fieldValues) throws Exception {
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            String name = components[i].getName();
            args[i] = fieldValues.get(name);
        }
        
        Class<?> recordClass = components[0].getDeclaringRecord();
        return recordClass.getConstructors()[0].newInstance(args);
    }
}