package net.mineabyss.core.storage.mongo.mapping;

import com.mineabyss.lib.bson.Document;
import com.mineabyss.lib.commands.util.TypeWrap;
import lombok.Getter;
import net.mineabyss.core.storage.mongo.mapping.exception.DeserializationException;
import net.mineabyss.core.storage.mongo.mapping.exception.SerializationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sophisticated DocumentMapper with type adapter system and recursive field processing
 * @param <T> Entity type
 */
public final class DocumentMapper<T> {

    private final TypeWrap<T> entityClass;
    @Getter private final TypeAdapterRegistry adapterRegistry;

    public DocumentMapper(TypeWrap<T> entityClass) {
        this(entityClass, new TypeAdapterRegistry());
    }

    public DocumentMapper(TypeWrap<T> entityClass, TypeAdapterRegistry adapterRegistry) {
        this.entityClass = entityClass;
        this.adapterRegistry = adapterRegistry;
    }

    /**
     * Convert entity to MongoDB Document
     */
    public Document toDocument(T entity) throws SerializationException {
        if (entity == null) {
            return null;
        }

        SerializationContextImpl context = new SerializationContextImpl(this);
        Object result = serializeValue(entity, context);

        if (result instanceof Document doc) {
            return doc;
        } else {
            throw new SerializationException("Root entity must serialize to Document");
        }
    }

    /**
     * Convert MongoDB Document to entity
     */
    public T fromDocument(Document document) throws DeserializationException {
        if (document == null) {
            return null;
        }

        DeserializationContextImpl context = new DeserializationContextImpl(this);
        return deserializeValue(document, entityClass, context);
    }

    /**
     * Internal method to serialize any value using appropriate adapter or recursive processing
     */
    @SuppressWarnings("unchecked")
    Object serializeValue(Object entityValue, SerializationContextImpl context) throws SerializationException {
        if (entityValue == null) {
            return null;
        }

        // Check for circular references
        if (context.isSerializing(entityValue)) {
            throw new SerializationException("Circular reference detected");
        }

        Class<?> valueClass = entityValue.getClass();

        // Try to find a type adapter
        TypeAdapter<Object> adapter = (TypeAdapter<Object>) adapterRegistry.findAdapter(TypeWrap.of(valueClass));
        if (adapter != null) {
            return adapter.serialize(entityValue, context);
        }

        // No adapter found, use recursive field processing
        return serializeObjectRecursively(entityValue, context);
    }

    private Object serializeObjectRecursively(Object value, SerializationContextImpl context) throws SerializationException {
        Class<?> valueClass = value.getClass();

        // Mark as currently serializing to detect cycles
        context.markSerializing(value);

        try {
            Document result = new Document();

            if (valueClass.isRecord()) {
                RecordFieldProcessor processor = new RecordFieldProcessor(valueClass);
                for (String fieldName : processor.getFieldNames()) {
                    try {
                        Object fieldValue = processor.getFieldValue(value, fieldName);
                        if (fieldValue != null) {
                            Object serializedValue = context.serialize(fieldValue);

                            // Handle ID field mapping
                            if ("id".equals(fieldName)) {
                                result.put("_id", serializedValue);
                            } else {
                                result.put(fieldName, serializedValue);
                            }
                        }
                    } catch (Exception e) {
                        throw new SerializationException("Failed to serialize field: " + fieldName, e);
                    }
                }
            } else {
                ClassFieldProcessor processor = new ClassFieldProcessor(valueClass);
                for (String fieldName : processor.getFieldNames()) {
                    try {
                        Object fieldValue = processor.getFieldValue(value, fieldName);
                        if (fieldValue != null) {
                            Object serializedValue = context.serialize(fieldValue);

                            // Handle ID field mapping
                            if ("id".equals(fieldName)) {
                                result.put("_id", serializedValue);
                            } else {
                                result.put(fieldName, serializedValue);
                            }
                        }
                    } catch (Exception e) {
                        throw new SerializationException("Failed to serialize field: " + fieldName, e);
                    }
                }
            }

            return result;
        } finally {
            context.unmarkSerializing(value);
        }
    }

    /**
     * Internal method to deserialize any value using appropriate adapter or recursive processing
     */
    <U> U deserializeValue(Object value, TypeWrap<U> targetType, DeserializationContextImpl context) throws DeserializationException {
        if (value == null) {
            return null;
        }

        // Try to find a type adapter
        TypeAdapter<U> adapter = adapterRegistry.findAdapter(targetType);
        if (adapter != null) {
            return adapter.deserialize(value, targetType, context);
        }

        // No adapter found, use recursive field processing
        return deserializeObjectRecursively(value, targetType, context);
    }

    @SuppressWarnings("unchecked")
    private <U> U deserializeObjectRecursively(Object value, TypeWrap<U> targetType, DeserializationContextImpl context) throws DeserializationException {
        if (!(value instanceof Document doc)) {
            throw new DeserializationException("Expected Document for object deserialization, got: " + value.getClass());
        }

        try {
            if (targetType.getRawType().isRecord()) {
                RecordFieldProcessor processor = new RecordFieldProcessor(targetType.getRawType());
                Map<String, Object> fieldValues = new HashMap<>();

                for (String fieldName : processor.getFieldNames()) {
                    Class<?> fieldType = processor.getFieldType(fieldName);
                    Object fieldValue;

                    // Handle ID field mapping
                    if ("id".equals(fieldName)) {
                        fieldValue = context.deserialize(doc.get("_id"), fieldType);
                    } else {
                        fieldValue = context.deserialize(doc.get(fieldName), fieldType);
                    }

                    fieldValues.put(fieldName, fieldValue);
                }

                return (U) processor.createInstance(fieldValues);
            } else {
                ClassFieldProcessor processor = new ClassFieldProcessor(targetType.getRawType());
                U instance = (U) targetType.getRawType().getDeclaredConstructor().newInstance();

                for (String fieldName : processor.getFieldNames()) {
                    Class<?> fieldType = processor.getFieldType(fieldName);
                    Object fieldValue;

                    // Handle ID field mapping
                    if ("id".equals(fieldName)) {
                        fieldValue = context.deserialize(doc.get("_id"), fieldType);
                    } else {
                        fieldValue = context.deserialize(doc.get(fieldName), fieldType);
                    }

                    if (fieldValue != null) {
                        processor.setFieldValue(instance, fieldName, fieldValue);
                    }
                }

                return instance;
            }
        } catch (Exception e) {
            throw new DeserializationException("Failed to deserialize object of type: " + targetType, e);
        }
    }
}