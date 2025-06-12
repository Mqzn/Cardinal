package net.mineabyss.core.storage.mysql;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles entity metadata for MySQL storage operations.
 * Caches reflection information for performance.
 */
public class MySQLEntityMetadata {
    
    private final ConcurrentHashMap<Class<?>, EntityInfo> entityInfoCache = new ConcurrentHashMap<>();

    private static class EntityInfo {
        final String tableName;
        final Field idField;

        EntityInfo(String tableName, Field idField) {
            this.tableName = tableName;
            this.idField = idField;
        }
    }
    
    public String getTableName(Class<?> entityClass) {
        return getEntityInfo(entityClass).tableName;
    }
    
    public Field getIdField(Class<?> entityClass) {
        return getEntityInfo(entityClass).idField;
    }
    
    private EntityInfo getEntityInfo(Class<?> entityClass) {
        return entityInfoCache.computeIfAbsent(entityClass, this::createEntityInfo);
    }
    
    private EntityInfo createEntityInfo(Class<?> entityClass) {
        String tableName = entityClass.getSimpleName().toLowerCase();
        Field idField = findIdField(entityClass);
        idField.setAccessible(true);
        return new EntityInfo(tableName, idField);
    }
    
    private Field findIdField(Class<?> clazz) {
        // Look for field named "id" first
        try {
            return clazz.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            // Look for any field that contains "id" in name
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().toLowerCase().contains("id")) {
                    return field;
                }
            }
            throw new RuntimeException("No ID field found in entity class: " + clazz.getName());
        }
    }
}
