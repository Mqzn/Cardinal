package eg.mqzen.cardinal.api.storage;

/**
 * Storage command interface for transaction execution
 */
@FunctionalInterface
public interface StorageCommand<T> {
    T execute() throws StorageException;
}