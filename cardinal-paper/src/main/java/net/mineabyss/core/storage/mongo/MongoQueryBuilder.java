package net.mineabyss.core.storage.mongo;

import com.mineabyss.lib.bson.Document;
import com.mineabyss.lib.bson.conversions.Bson;
import com.mineabyss.lib.mongo.client.FindIterable;
import com.mineabyss.lib.mongo.client.MongoCollection;
import net.mineabyss.cardinal.api.storage.QueryBuilder;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.storage.StorageMetrics;
import net.mineabyss.core.storage.mongo.mapping.DocumentMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB implementation of QueryBuilder
 *
 * @param <T> Entity type
 * @since 1.0
 */
public final class MongoQueryBuilder<T> implements QueryBuilder<T> {

    private final MongoCollection<Document> collection;
    private final DocumentMapper<T> mapper;
    private final StorageMetrics metrics;
    private final List<FilterGroup> filterGroups;
    private final List<Bson> sorts;
    private int limitValue = 0;
    private int skipValue = 0;
    private String currentField;
    private LogicalOperator nextOperator = LogicalOperator.AND;

    private enum LogicalOperator {
        AND, OR, NOT
    }

    // Helper class to track filters with their logical operators
    private record FilterGroup(Bson filter, LogicalOperator operator) { }

    public MongoQueryBuilder(MongoCollection<Document> collection, DocumentMapper<T> mapper, StorageMetrics metrics) {
        this.collection = collection;
        this.mapper = mapper;
        this.metrics = metrics;
        this.filterGroups = new ArrayList<>();
        this.sorts = new ArrayList<>();
    }

    @Override
    public QueryBuilder<T> where(String field) {
        this.currentField = field;
        return this;
    }

    @Override
    public QueryBuilder<T> eq(Object value) {
        addFilter(new Document(getCurrentField(), value));
        return this;
    }

    @Override
    public QueryBuilder<T> ne(Object value) {
        addFilter(new Document(getCurrentField(), new Document("$ne", value)));
        return this;
    }

    @Override
    public QueryBuilder<T> gt(Object value) {
        addFilter(new Document(getCurrentField(), new Document("$gt", value)));
        return this;
    }

    @Override
    public QueryBuilder<T> gte(Object value) {
        addFilter(new Document(getCurrentField(), new Document("$gte", value)));
        return this;
    }

    @Override
    public QueryBuilder<T> lt(Object value) {
        addFilter(new Document(getCurrentField(), new Document("$lt", value)));
        return this;
    }

    @Override
    public QueryBuilder<T> lte(Object value) {
        addFilter(new Document(getCurrentField(), new Document("$lte", value)));
        return this;
    }

    @Override
    public QueryBuilder<T> in(List<Object> values) {
        addFilter(new Document(getCurrentField(), new Document("$in", values)));
        return this;
    }

    @Override
    public QueryBuilder<T> like(String pattern) {
        addFilter(new Document(getCurrentField(), new Document("$regex", pattern).append("$options", "i")));
        return this;
    }

    @Override
    public QueryBuilder<T> and() {
        nextOperator = LogicalOperator.AND;
        return this;
    }

    @Override
    public QueryBuilder<T> or() {
        nextOperator = LogicalOperator.OR;
        return this;
    }

    @Override
    public QueryBuilder<T> not() {
        nextOperator = LogicalOperator.NOT;
        return this;
    }

    /*// Additional method to handle complex grouping
    public QueryBuilder<T> beginGroup() {
        // TODO This could be used for nested groupings like (A AND B) OR (C AND D)
        // TODO For now, we'll keep the simple implementation but this shows extensibility
        return this;
    }

    public QueryBuilder<T> endGroup() {
        // Corresponding end group method
        return this;
    }*/

    @Override
    public QueryBuilder<T> sortBy(String field, SortOrder order) {
        int sortOrder = order == SortOrder.ASC ? 1 : -1;
        sorts.add(new Document(field, sortOrder));
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
        try {
            long startTime = System.currentTimeMillis();

            FindIterable<Document> iterable = collection.find(buildQuery());

            if (!sorts.isEmpty()) {
                Document sortDoc = new Document();
                for (Bson sort : sorts) {
                    sortDoc.putAll(sort.toBsonDocument());
                }
                iterable = iterable.sort(sortDoc);
            }

            if (skipValue > 0) {
                iterable = iterable.skip(skipValue);
            }

            if (limitValue > 0) {
                iterable = iterable.limit(limitValue);
            }

            List<T> results = new ArrayList<>();
            for (Document doc : iterable) {
                results.add(mapper.fromDocument(doc));
            }

            metrics.recordOperation("query", System.currentTimeMillis() - startTime);
            return results;
        } catch (Exception e) {
            metrics.recordError("query");
            throw new StorageException("Query execution failed", e);
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
        List<T> results = limit(1).execute();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public long count() throws StorageException {
        try {
            long startTime = System.currentTimeMillis();
            long count = collection.countDocuments(buildQuery());
            metrics.recordOperation("count", System.currentTimeMillis() - startTime);
            return count;
        } catch (Exception e) {
            metrics.recordError("count");
            throw new StorageException("Count query failed", e);
        }
    }

    private void addFilter(Document filter) {
        // Apply NOT operator by wrapping the filter
        Bson finalFilter = (nextOperator == LogicalOperator.NOT)
                ? new Document("$not", filter)
                : filter;

        filterGroups.add(new FilterGroup(finalFilter, nextOperator));

        // Reset to default AND after adding the filter
        nextOperator = LogicalOperator.AND;
    }

    private String getCurrentField() {
        if (currentField == null) {
            throw new IllegalStateException("No field specified for query condition");
        }
        return "id".equals(currentField) ? "_id" : currentField;
    }

    private Bson buildQuery() {
        if (filterGroups.isEmpty()) {
            return new Document();
        }

        if (filterGroups.size() == 1) {
            return filterGroups.getFirst().filter;
        }

        // Group filters by their logical operators
        List<Bson> andFilters = new ArrayList<>();
        List<Bson> orFilters = new ArrayList<>();

        for (FilterGroup group : filterGroups) {
            switch (group.operator) {
                case AND:
                case NOT: // NOT is already applied to the filter itself
                    andFilters.add(group.filter);
                    break;
                case OR:
                    orFilters.add(group.filter);
                    break;
            }
        }

        // Build the final query based on the operators used
        if (!orFilters.isEmpty() && !andFilters.isEmpty()) {
            // Mixed AND/OR: combine them appropriately
            List<Bson> finalConditions = new ArrayList<>();

            if (andFilters.size() == 1) {
                finalConditions.add(andFilters.getFirst());
            } else {
                finalConditions.add(new Document("$and", andFilters));
            }

            finalConditions.addAll(orFilters);

            return new Document("$or", finalConditions);
        } else if (!orFilters.isEmpty()) {
            // Only OR conditions
            return new Document("$or", orFilters);
        } else {
            // Only AND conditions (default behavior)
            return new Document("$and", andFilters);
        }
    }

    // Additional utility methods that could be useful

    /**
     * Checks if the current query has any filters
     */
    public boolean hasFilters() {
        return !filterGroups.isEmpty();
    }

    /**
     * Clears all filters and sorting, effectively resetting the query
     */
    public QueryBuilder<T> reset() {
        filterGroups.clear();
        sorts.clear();
        limitValue = 0;
        skipValue = 0;
        currentField = null;
        nextOperator = LogicalOperator.AND;
        return this;
    }

    /**
     * Returns a copy of this query builder for creating variations
     */
    public MongoQueryBuilder<T> copy() {
        MongoQueryBuilder<T> copy = new MongoQueryBuilder<>(collection, mapper, metrics);
        copy.filterGroups.addAll(this.filterGroups);
        copy.sorts.addAll(this.sorts);
        copy.limitValue = this.limitValue;
        copy.skipValue = this.skipValue;
        copy.currentField = this.currentField;
        copy.nextOperator = this.nextOperator;
        return copy;
    }
}