package net.mineabyss.cardinal.api.config;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a message key with hierarchical structure support
 */
public interface MessageKey {
    /**
     * Gets the full path of this message key (e.g., "punishments.ban.kick_message")
     */
    @NotNull String getPath();
    
    /**
     * Gets the default message if none is found in config
     */
    String getDefaultMessage();
    
    /**
     * Gets the parent key if this is a nested key
     */
    MessageKey getParent();
}
