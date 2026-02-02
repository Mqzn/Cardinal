package eg.mqzen.cardinal.storage;

import eg.mqzen.cardinal.api.storage.StorageMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StorageMetricsImpl implements StorageMetrics {

    private final Map<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();

    private static class OperationMetrics {
        private final AtomicLong operationCount = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);

        void recordOperation(long durationMs) {
            operationCount.incrementAndGet();
            totalDuration.addAndGet(durationMs);
        }

        void recordError() {
            errorCount.incrementAndGet();
        }

        long getOperationCount() {
            return operationCount.get();
        }

        double getAverageExecutionTime() {
            long count = operationCount.get();
            return count == 0 ? 0.0 : (double) totalDuration.get() / count;
        }

        long getErrorCount() {
            return errorCount.get();
        }
    }

    @Override
    public void recordOperation(String operation, long durationMs) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation name cannot be null");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        operationMetrics.computeIfAbsent(operation, k -> new OperationMetrics())
                .recordOperation(durationMs);
    }

    @Override
    public void recordError(String operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation name cannot be null");
        }

        operationMetrics.computeIfAbsent(operation, k -> new OperationMetrics())
                .recordError();
    }

    @Override
    public long getOperationCount(String operation) {
        if (operation == null) {
            return 0;
        }

        OperationMetrics metrics = operationMetrics.get(operation);
        return metrics != null ? metrics.getOperationCount() : 0;
    }

    @Override
    public double getAverageExecutionTime(String operation) {
        if (operation == null) {
            return 0.0;
        }

        OperationMetrics metrics = operationMetrics.get(operation);
        return metrics != null ? metrics.getAverageExecutionTime() : 0.0;
    }

    @Override
    public long getErrorCount(String operation) {
        if (operation == null) {
            return 0;
        }

        OperationMetrics metrics = operationMetrics.get(operation);
        return metrics != null ? metrics.getErrorCount() : 0;
    }

    @Override
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> allMetrics = new HashMap<>();

        for (Map.Entry<String, OperationMetrics> entry : operationMetrics.entrySet()) {
            String operation = entry.getKey();
            OperationMetrics metrics = entry.getValue();

            Map<String, Object> operationData = new HashMap<>();
            operationData.put("operationCount", metrics.getOperationCount());
            operationData.put("averageExecutionTime", metrics.getAverageExecutionTime());
            operationData.put("errorCount", metrics.getErrorCount());
            operationData.put("totalDuration", metrics.totalDuration.get());

            allMetrics.put(operation, operationData);
        }

        return allMetrics;
    }

    /**
     * Resets all metrics for the specified operation
     * @param operation the operation name
     */
    public void resetMetrics(String operation) {
        if (operation != null) {
            operationMetrics.remove(operation);
        }
    }

    /**
     * Resets all metrics for all operations
     */
    public void resetAllMetrics() {
        operationMetrics.clear();
    }

    /**
     * Gets the success rate for an operation (successful operations / total operations)
     * @param operation the operation name
     * @return success rate as a percentage (0.0 to 1.0)
     */
    public double getSuccessRate(String operation) {
        if (operation == null) {
            return 0.0;
        }

        OperationMetrics metrics = operationMetrics.get(operation);
        if (metrics == null) {
            return 0.0;
        }

        long totalOps = metrics.getOperationCount();
        long errors = metrics.getErrorCount();

        if (totalOps == 0) {
            return 0.0;
        }

        return (double) (totalOps - errors) / totalOps;
    }
}