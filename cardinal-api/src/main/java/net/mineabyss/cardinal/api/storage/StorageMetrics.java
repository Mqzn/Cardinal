package net.mineabyss.cardinal.api.storage;

import java.util.Map;

/**
 * Storage metrics interface
 * 
 * @since 1.0
 */
public interface StorageMetrics {
    void recordOperation(String operation, long durationMs);
    void recordError(String operation);
    long getOperationCount(String operation);
    double getAverageExecutionTime(String operation);
    long getErrorCount(String operation);
    Map<String, Object> getAllMetrics();
}