package net.mineabyss.cardinal.api.storage;

import com.mineabyss.lib.config.YamlDocument;

/**
 * Factory for creating storage engines based on configuration
 */
public interface StorageEngineFactory {

    StorageEngine create(StorageConfig config) throws StorageException;

    StorageEngine createFromYaml(YamlDocument yaml) throws StorageException;

}