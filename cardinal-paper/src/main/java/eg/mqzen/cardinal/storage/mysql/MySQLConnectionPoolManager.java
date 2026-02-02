package eg.mqzen.cardinal.storage.mysql;

import eg.mqzen.lib.hikari.HikariConfig;
import eg.mqzen.lib.hikari.HikariDataSource;
import eg.mqzen.cardinal.api.storage.StorageConfig;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Manages MySQL connection pooling with optimized settings for performance.
 */
public class MySQLConnectionPoolManager {
    
    public static DataSource createOptimizedDataSource(StorageConfig.MySQLConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // Basic connection settings
        hikariConfig.setJdbcUrl(config.url());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool settings
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setMinimumIdle(Math.max(1, config.poolSize() / 4));
        hikariConfig.setConnectionTimeout(config.connectionTimeoutMs());
        hikariConfig.setIdleTimeout(600000); // 10 minutes
        hikariConfig.setMaxLifetime(config.maxLifetime());
        hikariConfig.setLeakDetectionThreshold(60000); // 1 minute
        
        // Performance optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        // Connection validation
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setValidationTimeout(5000);
        
        // Auto-commit setting
        hikariConfig.setAutoCommit(config.autoCommit());
        
        return new HikariDataSource(hikariConfig);
    }
    
    public static boolean testConnection(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}