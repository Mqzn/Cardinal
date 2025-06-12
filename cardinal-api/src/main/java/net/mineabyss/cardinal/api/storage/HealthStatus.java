package net.mineabyss.cardinal.api.storage;

/**
 * Health status record
 * 
 * @since 1.0
 */
public record HealthStatus(
    boolean healthy,
    String message,
    String error
) {}