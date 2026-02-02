package eg.mqzen.cardinal.api.storage;

/**
 * Main configuration record for storage settings
 */
public record StorageConfig(
    StorageType type,
    MongoConfig mongo,
    MySQLConfig mysql
) {
    public record MongoConfig(
        String uri,
        String database,
        String collectionPrefix,
        int maxPoolSize,
        int minPoolSize,
        int connectionTimeoutMs
    ) {
        public MongoConfig {
            if (uri == null || uri.isBlank()) {
                throw new IllegalArgumentException("MongoDB URI cannot be null or blank");
            }
            if (database == null || database.isBlank()) {
                throw new IllegalArgumentException("MongoDB database cannot be null or blank");
            }
        }
    }
    
    public record MySQLConfig(
        String url,
        String username,
        String password,
        String tablePrefix,
        int poolSize,
        int connectionTimeoutMs,
        int maxLifetime,
        boolean autoCommit
    ) {
        public MySQLConfig {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("MySQL URL cannot be null or blank");
            }
            if (username == null) {
                throw new IllegalArgumentException("MySQL username cannot be null");
            }
        }
    }
}