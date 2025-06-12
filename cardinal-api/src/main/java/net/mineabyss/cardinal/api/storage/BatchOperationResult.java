package net.mineabyss.cardinal.api.storage;

import java.util.List;

/**
 * Batch operation result
 * 
 * @since 1.0
 */
public record BatchOperationResult(
    int insertedCount,
    int updatedCount,
    int deletedCount,
    List<String> errors
) {}