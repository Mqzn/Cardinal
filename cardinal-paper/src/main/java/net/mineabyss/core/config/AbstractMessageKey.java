package net.mineabyss.core.config;

import net.mineabyss.cardinal.api.config.MessageKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base class for message keys providing common functionality
 */
abstract class AbstractMessageKey implements MessageKey {
    protected final String path;
    protected final String defaultMessage;
    protected final MessageKey parent;
    
    protected AbstractMessageKey(String path, String defaultMessage, MessageKey parent) {
        this.path = path;
        this.defaultMessage = defaultMessage;
        this.parent = parent;
    }
    
    @NotNull @Override
    public String getPath() {
        return path;
    }
    
    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    @Override
    public MessageKey getParent() {
        return parent;
    }
    
    @Override
    public String toString() {
        return getPath();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MessageKey)) {
            return false;
        }
        return Objects.equals(path, ((MessageKey) obj).getPath());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}