package net.mineabyss.core.storage;

import com.mineabyss.lib.config.YamlDocument;
import net.mineabyss.cardinal.api.storage.StorageConfig;
import net.mineabyss.cardinal.api.storage.StorageEngine;
import net.mineabyss.cardinal.api.storage.StorageEngineFactory;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.storage.StorageType;

/**
 * Convenience class providing static factory methods.
 * This is the recommended way to create storage engines.
 * 
 * @since 1.0
 */
public final class StorageEngines {
    
    private static final StorageEngineFactory factory = DefaultStorageEngineFactory.getInstance();

    private final static String DEFAULT_PREFIX = "cardinal_";

    private StorageEngines() {}
    
    /**
     * Create a storage engine from configuration
     */
    public static StorageEngine create(StorageConfig config) throws StorageException {
        return factory.create(config);
    }
    
    /**
     * Create a storage engine from YAML file
     */
    public static StorageEngine createFromYaml(YamlDocument yamlDocument) throws StorageException {
        return factory.createFromYaml(yamlDocument);
    }
    
    /**
     * Create a MongoDB storage engine with default settings
     */
    public static StorageEngine createMongo(String uri, String database) throws StorageException {
        StorageConfig config = new StorageConfig(
            StorageType.MONGO,
            new StorageConfig.MongoConfig(uri, database, DEFAULT_PREFIX, 20, 5, 30000),
            null
        );
        return create(config);
    }
    
    /**
     * Create a MySQL storage engine with default settings
     */
    public static StorageEngine createMySQL(String url, String username, String password) throws StorageException {
        StorageConfig config = new StorageConfig(
            StorageType.MYSQL,
            null,
            new StorageConfig.MySQLConfig(url, username, password, "cardinal_", 10, 30000, 1800000, false)
        );
        return create(config);
    }
}