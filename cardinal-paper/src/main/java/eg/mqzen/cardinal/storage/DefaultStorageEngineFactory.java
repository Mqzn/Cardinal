package eg.mqzen.cardinal.storage;


import eg.mqzen.lib.config.YamlDocument;
import eg.mqzen.cardinal.api.storage.StorageConfig;
import eg.mqzen.cardinal.api.storage.StorageEngine;
import eg.mqzen.cardinal.api.storage.StorageEngineFactory;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.api.storage.StorageType;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.storage.mongo.MongoStorageEngine;
import eg.mqzen.cardinal.storage.mysql.MySQLStorageEngine;

/**
 * Default implementation of StorageEngineFactory
 * 
 * @since 1.0
 */
public final class DefaultStorageEngineFactory implements StorageEngineFactory {
    
    private static final StorageEngineFactory INSTANCE = new DefaultStorageEngineFactory();
    
    private DefaultStorageEngineFactory() {}
    
    public static StorageEngineFactory getInstance() {
        return INSTANCE;
    }
    
    @Override
    public StorageEngine create(StorageConfig config) throws StorageException {
        return switch (config.type()) {
            case MONGO -> new MongoStorageEngine(config.mongo());
            case MYSQL -> new MySQLStorageEngine(config.mysql());
        };
    }

    private final static String GLOBAL_STORAGE_PATH = "storage.";
    private final static String GLOBAL_MONGO_PATH =  GLOBAL_STORAGE_PATH.concat("mongo.");
    private final static String GLOBAL_MYSQL_PATH = GLOBAL_STORAGE_PATH.concat("mysql.");

    @Override
    public StorageEngine createFromYaml(YamlDocument yaml) throws StorageException {
        /*
            storage:
                type: MONGO
                mongo:
                    uri: "mongodb://localhost:27017"
                    database: "app_db"
                    collectionPrefix: "app_"
                    maxPoolSize: 20
                    minPoolSize: 5
                    connectionTimeoutMs: 30000
                mysql:
                    url: "jdbc:mysql://localhost:3306/app_db"
                    username: "user"
                    password: "password"
                    poolSize: 10
                    connectionTimeoutMs: 30000
                    maxLifetime: 1800000
                    autoCommit: false
        */
        try {
            StorageConfig config = loadStorageConfig(yaml);
            if( config == null) {
                Cardinal.severe("Failed to create StorageConfig instance.");
                throw new StorageException("Failed to create storage engine from config.yml");
            }
            return create(config);
        } catch (Exception e) {
            throw new StorageException("Failed to create storage engine from config.yml", e);
        }
    }

    private static StorageConfig loadStorageConfig(YamlDocument yamlDocument) {
        String typeStr = yamlDocument.getString(GLOBAL_STORAGE_PATH + "type");
        try {
            StorageType storageType = StorageType.valueOf(typeStr);

            StorageConfig.MongoConfig mongoConfig = new StorageConfig.MongoConfig(
                    yamlDocument.getString(GLOBAL_MONGO_PATH + "uri"),
                    yamlDocument.getString(GLOBAL_MONGO_PATH + "database"),
                    yamlDocument.getString(GLOBAL_MONGO_PATH + "collectionPrefix", ""),
                    yamlDocument.getInt(GLOBAL_MONGO_PATH + "maxPoolSize", 20),
                    yamlDocument.getInt(GLOBAL_MONGO_PATH + "minPoolSize", 5),
                    yamlDocument.getInt(GLOBAL_MONGO_PATH + "connectionTimeoutMs", 30000)
            );

            StorageConfig.MySQLConfig mySQLConfig = new StorageConfig.MySQLConfig(
                    yamlDocument.getString(GLOBAL_MYSQL_PATH + "url"),
                    yamlDocument.getString(GLOBAL_MYSQL_PATH + "username"),
                    yamlDocument.getString(GLOBAL_MYSQL_PATH + "password", ""),
                    yamlDocument.getString(GLOBAL_MYSQL_PATH + "table-prefix", "cardinal_"),
                    yamlDocument.getInt(GLOBAL_MYSQL_PATH + "poolSize", 10),
                    yamlDocument.getInt(GLOBAL_MYSQL_PATH + "connectionTimeoutMs", 30000),
                    yamlDocument.getInt(GLOBAL_MYSQL_PATH + "maxLifetime", 1800000),
                    yamlDocument.getBoolean(GLOBAL_MYSQL_PATH + "autoCommit", false)
            );

            return new StorageConfig(storageType, mongoConfig, mySQLConfig);

        }catch (EnumConstantNotPresentException ex) {
            Cardinal.severe("Invalid storage type '%s'",typeStr);
            return null;
        }
    }
}