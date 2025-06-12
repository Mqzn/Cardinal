package net.mineabyss.core.storage.mongo.mapping;

import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.core.storage.mongo.mapping.adapter.ArrayTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.BooleanTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.CollectionTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.MapTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.NumberTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.ObjectIdTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.StringTypeAdapter;
import net.mineabyss.core.storage.mongo.mapping.adapter.punishment.PunishmentAdapter;
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

        System.out.println("Finding adapter for type '" + type.getTypeName() + "'");

        // Find adapter
        for (TypeAdapter<?> adapter : adapters) {
            System.out.println("Checking adapter '" + adapter.getClass().getName() + "'");
            if (adapter.canHandle(typeWrap)) {
                return (TypeAdapter<T>) adapter;
            }
        }
        System.out.println("No adapter found for type '" + type.getTypeName() + "'");
        return null;
    }
    
    /**
     * Check if there's an adapter for the given type
     */
    public boolean hasAdapter(TypeWrap<?> type) {
        return findAdapter(type) != null;
    }
}