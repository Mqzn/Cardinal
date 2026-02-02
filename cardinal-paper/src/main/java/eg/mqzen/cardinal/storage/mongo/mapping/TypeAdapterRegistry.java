package eg.mqzen.cardinal.storage.mongo.mapping;

import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.ArrayTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.BooleanTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.CollectionTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.MapTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.NumberTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.ObjectIdTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.StringTypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.adapter.punishment.PunishmentAdapter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry for managing type adapters with support for registration and lookup
 */
public class TypeAdapterRegistry {
    private final List<TypeAdapter<?>> adapters = new ArrayList<>();
    
    public TypeAdapterRegistry() {
        registerBuiltInAdapters();
    }
    
    private void registerBuiltInAdapters() {
        register(new PunishmentAdapter());
        register(new StringTypeAdapter());
        register(new NumberTypeAdapter());
        register(new BooleanTypeAdapter());
        register(new ObjectIdTypeAdapter());
        register(new CollectionTypeAdapter());
        register(new MapTypeAdapter());
        register(new ArrayTypeAdapter());
    }
    
    /**
     * Register a custom type adapter
     */
    public <T> void register(TypeAdapter<T> adapter) {
        adapters.add(adapter);
    }
    
    /**
     * Find an appropriate type adapter for the given type
     */
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> findAdapter(TypeWrap<T> typeWrap) {
        Type type = typeWrap.getType();


        // Find adapter
        for (TypeAdapter<?> adapter : adapters) {
            if (adapter.canHandle(typeWrap)) {
                return (TypeAdapter<T>) adapter;
            }
        }
        return null;
    }
    
    /**
     * Check if there's an adapter for the given type
     */
    public boolean hasAdapter(TypeWrap<?> type) {
        return findAdapter(type) != null;
    }
}