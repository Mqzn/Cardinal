package net.mineabyss.core.storage;

import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.cardinal.api.storage.DBEntity;
import net.mineabyss.cardinal.api.storage.QueryBuilder;
import net.mineabyss.cardinal.api.storage.Repository;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.storage.StorageMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MultiRepositoryQueryBuilder<T extends DBEntity<?>> implements QueryBuilder<T> {

    private final List<Repository<?, T>> repositories;
    private final TypeWrap<T> entityClass;
    private final StorageMetrics metrics;
    private final List<QueryBuilder<T>> builders;

    private final List<Function<QueryBuilder<T>, QueryBuilder<T>>> operations = new ArrayList<>();
    private QueryBuilder.SortOrder sortOrder;
    private String sortField;
    private int limitValue = -1;
    private int skipValue = 0;

    public MultiRepositoryQueryBuilder(
            List<Repository<?, T>> repositories,
            TypeWrap<T> entityClass,
            StorageMetrics metrics
    ) {
        this.repositories = repositories;
        this.entityClass = entityClass;
        this.metrics = metrics;
        this.builders = new ArrayList<>();

        for (Repository<?, T> repo : repositories) {
            builders.add(repo.query());
        }
    }

    private void applyOperation(Function<QueryBuilder<T>, QueryBuilder<T>> operation) {
        operations.add(operation);
        for (QueryBuilder<T> builder : builders) {
            operation.apply(builder);
        }
    }

    @Override
    public QueryBuilder<T> where(String field) {
        applyOperation(b -> b.where(field));
        return this;
    }

    @Override
    public QueryBuilder<T> eq(Object value) {
        applyOperation(b -> b.eq(value));
        return this;
    }

    @Override
    public QueryBuilder<T> ne(Object value) {
        applyOperation(b -> b.ne(value));
        return this;
    }

    @Override
    public QueryBuilder<T> gt(Object value) {
        applyOperation(b -> b.gt(value));
        return this;
    }

    @Override
    public QueryBuilder<T> gte(Object value) {
        applyOperation(b -> b.gte(value));
        return this;
    }

    @Override
    public QueryBuilder<T> lt(Object value) {
        applyOperation(b -> b.lt(value));
        return this;
    }

    @Override
    public QueryBuilder<T> lte(Object value) {
        applyOperation(b -> b.lte(value));
        return this;
    }

    @Override
    public QueryBuilder<T> in(List<Object> values) {
        applyOperation(b -> b.in(values));
        return this;
    }

    @Override
    public QueryBuilder<T> like(String pattern) {
        applyOperation(b -> b.like(pattern));
        return this;
    }

    @Override
    public QueryBuilder<T> and() {
        applyOperation(QueryBuilder::and);
        return this;
    }

    @Override
    public QueryBuilder<T> or() {
        applyOperation(QueryBuilder::or);
        return this;
    }

    @Override
    public QueryBuilder<T> not() {
        applyOperation(QueryBuilder::not);
        return this;
    }

    @Override
    public QueryBuilder<T> sortBy(String field, SortOrder order) {
        this.sortField = field;
        this.sortOrder = order;
        applyOperation(b -> b.sortBy(field, order));
        return this;
    }

    @Override
    public QueryBuilder<T> limit(int limit) {
        if(limit == -1) return this;
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
            List<T> results = new ArrayList<>();

            for (Repository<?, T> repo : repositories) {
                QueryBuilder<T> builder = repo.query();
                // Reapply all operations
                for (Function<QueryBuilder<T>, QueryBuilder<T>> op : operations) {
                    builder = op.apply(builder);
                }
                results.addAll(builder.execute());
            }

            // Apply global sorting and limiting
            results = applyGlobalSortAndLimit(results);

            metrics.recordOperation("multi-query", System.currentTimeMillis() - startTime);
            return results;
        } catch (Exception e) {
            metrics.recordError("multi-query");
            throw new StorageException("Multi-repository query failed", e);
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
        long total = 0;
        for (Repository<?, T> repo : repositories) {
            QueryBuilder<T> builder = repo.query();
            // Reapply all operations
            for (Function<QueryBuilder<T>, QueryBuilder<T>> op : operations) {
                builder = op.apply(builder);
            }
            total += builder.count();
        }
        return total;
    }

    private List<T> applyGlobalSortAndLimit(List<T> results) {
        // Apply sorting if specified
        if (sortField != null && sortOrder != null) {
            results.sort((a, b) -> {
                try {
                    java.lang.reflect.Field field = entityClass.getRawType().getDeclaredField(sortField);
                    field.setAccessible(true);
                    Comparable valA = (Comparable) field.get(a);
                    Comparable valB = (Comparable) field.get(b);

                    if (valA == null && valB == null) return 0;
                    if (valA == null) return sortOrder == SortOrder.ASC ? -1 : 1;
                    if (valB == null) return sortOrder == SortOrder.ASC ? 1 : -1;

                    int comparison = valA.compareTo(valB);
                    return sortOrder == SortOrder.ASC ? comparison : -comparison;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to sort results", e);
                }
            });
        }

        // Apply skip and limit
        if (skipValue > 0) {
            results = results.stream().skip(skipValue).collect(Collectors.toList());
        }
        if (limitValue > 0 && results.size() > limitValue) {
            results = results.subList(0, limitValue);
        }

        return results;
    }

}