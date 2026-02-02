package eg.mqzen.cardinal.api.storage;

/**
 * Storage observer for event notifications
 */
public interface StorageObserver {
    void onStorageEvent(StorageEvent event);
}