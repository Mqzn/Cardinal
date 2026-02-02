package eg.mqzen.cardinal.api.storage;

/**
 * Storage exception class
 */
public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}