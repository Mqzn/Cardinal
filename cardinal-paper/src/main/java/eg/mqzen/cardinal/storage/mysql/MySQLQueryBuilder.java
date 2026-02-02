package eg.mqzen.cardinal.storage.mysql;

import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.cardinal.api.storage.QueryBuilder;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.api.storage.StorageMetrics;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

/**
 * MySQL implementation of the QueryBuilder interface.
 * Uses JSON path expressions for querying JSON data stored in MySQL.
 */
public class MySQLQueryBuilder<T> implements QueryBuilder<T> {
    
    private final TypeWrap<T> entityClass;
    private final DataSource dataSource;
    private final String tableName;
    private final ObjectMapper objectMapper;
    private final StorageMetrics metrics;
    
    private final StringBuilder whereClause = new StringBuilder();
    private final List<Object> parameters = new ArrayList<>();
    private String currentField;
    private String orderBy;
    private Integer limitValue;
    private Integer skipValue;
    
    public MySQLQueryBuilder(TypeWrap<T> entityClass, DataSource dataSource, String tableName,
                            ObjectMapper objectMapper, StorageMetrics metrics) {
        this.entityClass = entityClass;
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }
    
    @Override
    public QueryBuilder<T> where(String field) {
        this.currentField = field;
        return this;
    }
    
    @Override
    public QueryBuilder<T> eq(Object value) {
        addCondition("JSON_EXTRACT(data, '$.%s') = ?".formatted(currentField), value);
        return this;
    }
    
    @Override
    public QueryBuilder<T> ne(Object value) {
        addCondition("JSON_EXTRACT(data, '$.%s') != ?".formatted(currentField), value);
        return this;
    }
    
    @Override
    public QueryBuilder<T> gt(Object value) {
        addCondition("JSON_EXTRACT(data, '$.%s') > ?".formatted(currentField), value);
        return this;
    }
    
    @Override
    public QueryBuilder<T> gte(Object value) {
        addCondition("JSON_EXTRACT(data, '$.%s') >= ?".formatted(currentField), value);
        return this;
    }
    
    @Override
    public QueryBuilder<T> lt(Object value) {
        addCondition("JSON_EXTRACT(data, '$.%s') < ?".formatted(currentField), value);
        return this;
    }
    
    @Override
    public QueryBuilder<T> lte(Object value) {
        addCondition("JSON_EXTRACT(data, '$.%s') <= ?".formatted(currentField), value);
        return this;
    }
    
    @Override
    public QueryBuilder<T> in(List<Object> values) {
        String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
        addCondition("JSON_EXTRACT(data, '$.%s') IN (%s)".formatted(currentField, placeholders), values);
        return this;
    }
    
    @Override
    public QueryBuilder<T> like(String pattern) {
        addCondition("JSON_UNQUOTE(JSON_EXTRACT(data, '$.%s')) LIKE ?".formatted(currentField), pattern);
        return this;
    }
    
    @Override
    public QueryBuilder<T> and() {
        if (!whereClause.isEmpty()) {
            whereClause.append(" AND ");
        }
        return this;
    }
    
    @Override
    public QueryBuilder<T> or() {
        if (!whereClause.isEmpty()) {
            whereClause.append(" OR ");
        }
        return this;
    }
    
    @Override
    public QueryBuilder<T> not() {
        whereClause.append("NOT ");
        return this;
    }
    
    @Override
    public QueryBuilder<T> sortBy(Class<?> sortEntityTypeClass, String field, QueryBuilder.SortOrder order) {
        this.orderBy = "JSON_EXTRACT(data, '$.%s') %s".formatted(field, order.name());
        return this;
    }
    
    @Override
    public QueryBuilder<T> limit(int limit) {
        this.limitValue = limit;
        return this;
    }
    
    @Override
    public QueryBuilder<T> skip(int skip) {
        this.skipValue = skip;
        return this;
    }
    
    @Override
    public List<T> execute() throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = buildQuery("SELECT data FROM %s".formatted(tableName));
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameters(stmt);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next()) {
                        String json = rs.getString("data");
                        T entity = (T) objectMapper.readValue(json, entityClass.getRawType());
                        results.add(entity);
                    }
                    
                    metrics.recordOperation("query", System.currentTimeMillis() - startTime);
                    return results;
                }
            }
            
        } catch (Exception e) {
            metrics.recordError("query");
            throw new StorageException("Failed to execute query", e);
        }
    }
    
    @Override
    public CompletableFuture<List<T>> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public Optional<T> findFirst() throws StorageException {
        QueryBuilder<T> limitedQuery = limit(1);
        List<T> results = limitedQuery.execute();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }
    
    @Override
    public long count() throws StorageException {
        long startTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            String sql = buildQuery("SELECT COUNT(*) FROM %s".formatted(tableName));
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameters(stmt);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        metrics.recordOperation("count", System.currentTimeMillis() - startTime);
                        return count;
                    }
                    return 0;
                }
            }
            
        } catch (Exception e) {
            metrics.recordError("count");
            throw new StorageException("Failed to count query results", e);
        }
    }
    
    private void addCondition(String condition, Object value) {
        if (!whereClause.isEmpty() && !whereClause.toString().endsWith(" AND ") && !whereClause.toString().endsWith(" OR ")) {
            whereClause.append(" AND ");
        }
        whereClause.append(condition);
        
        if (value instanceof List) {
            parameters.addAll((List<?>) value);
        } else {
            parameters.add(value);
        }
    }
    
    private String buildQuery(String baseQuery) {
        StringBuilder query = new StringBuilder(baseQuery);
        
        if (!whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        if (orderBy != null) {
            query.append(" ORDER BY ").append(orderBy);
        }
        
        if (limitValue != null) {
            query.append(" LIMIT ").append(limitValue);
        }
        
        if (skipValue != null) {
            query.append(" OFFSET ").append(skipValue);
        }
        
        return query.toString();
    }
    
    private void setParameters(PreparedStatement stmt) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            switch (param) {
                case String s -> stmt.setString(i + 1, s);
                case Integer integer -> stmt.setInt(i + 1, integer);
                case Long l -> stmt.setLong(i + 1, l);
                case Double v -> stmt.setDouble(i + 1, v);
                case Boolean b -> stmt.setBoolean(i + 1, b);
                default -> stmt.setString(i + 1, param.toString());
            }
        }
    }
}