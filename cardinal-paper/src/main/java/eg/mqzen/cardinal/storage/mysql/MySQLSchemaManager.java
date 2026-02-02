package eg.mqzen.cardinal.storage.mysql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages MySQL schema operations including table creation and validation.
 */
public class MySQLSchemaManager {
    
    private final DataSource dataSource;
    private final Set<String> createdTables = new HashSet<>();
    
    public MySQLSchemaManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void ensureTableExists(String tableName) {
        if (createdTables.contains(tableName)) {
            return;
        }
        
        synchronized (this) {
            if (createdTables.contains(tableName)) {
                return;
            }
            
            if (!tableExists(tableName)) {
                createTable(tableName);
            }
            
            createdTables.add(tableName);
        }
    }
    
    private boolean tableExists(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                SELECT COUNT(*) FROM information_schema.tables 
                WHERE table_schema = DATABASE() AND table_name = ?
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if table exists: " + tableName, e);
        }
    }
    
    private void createTable(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                CREATE TABLE %s (
                    id VARCHAR(255) PRIMARY KEY,
                    data JSON NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_created_at (created_at),
                    INDEX idx_updated_at (updated_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """.formatted(tableName);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create table: " + tableName, e);
        }
    }
    
    public void createJsonIndex(String tableName, String fieldPath, String indexName) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                CREATE INDEX %s ON %s ((JSON_EXTRACT(data, '$.%s')))
                """.formatted(indexName, tableName, fieldPath);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            // Index might already exist, log but don't fail
            System.err.println("Failed to create JSON index: " + e.getMessage());
        }
    }
}