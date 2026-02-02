package eg.mqzen.cardinal.config;

import eg.mqzen.cardinal.api.config.MessageKey;
import org.jetbrains.annotations.NotNull;

/**
 * Container for related message keys with hierarchical organization
 */
public abstract class MessageKeyContainer implements MessageKey {
    protected final String path;
    protected final MessageKey parent;
    
    protected MessageKeyContainer(String path, MessageKey parent) {
        this.path = parent != null ? parent.getPath() + "." + path : path;
        this.parent = parent;
    }
    
    @NotNull @Override
    public String getPath() {
        return path;
    }
    
    @Override
    public String getDefaultMessage() {
        return ""; // Containers don't have default messages
    }
    
    @Override
    public MessageKey getParent() {
        return parent;
    }
    
    /**
     * Creates a child message key
     */
    protected MessageKey createKey(String subPath, String defaultMessage) {
        return SimpleMessageKey.of(subPath, defaultMessage, this);
    }
}