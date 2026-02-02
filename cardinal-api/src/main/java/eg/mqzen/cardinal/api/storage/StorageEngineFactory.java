package eg.mqzen.cardinal.api.storage;

import eg.mqzen.lib.config.YamlDocument;

/**
 * Factory for creating storage engines based on configuration
 */
public interface StorageEngineFactory {

    StorageEngine create(StorageConfig config) throws StorageException;

    StorageEngine createFromYaml(YamlDocument yaml) throws StorageException;

}