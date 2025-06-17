package net.mineabyss.core.config;

import net.mineabyss.cardinal.api.config.MessageKey;

/**
 * Simple implementation of MessageKey for leaf nodes
 */
public final class SimpleMessageKey extends AbstractMessageKey {
    private SimpleMessageKey(String path, String defaultMessage) {
        super(path, defaultMessage, null);
    }

    private SimpleMessageKey(String path, String defaultMessage, MessageKey parent) {
        super(buildPath(parent, path), defaultMessage, parent);
    }

    /**
     * Creates a root message key without parent
     */
    public static SimpleMessageKey of(String path, String defaultMessage) {
        return new SimpleMessageKey(path, defaultMessage);
    }

    /**
     * Creates a child message key with parent
     */
    public static SimpleMessageKey of(String path, String defaultMessage, MessageKey parent) {
        return new SimpleMessageKey(path, defaultMessage, parent);
    }

    /**
     * Creates a message key with empty default message
     */
    public static SimpleMessageKey of(String path) {
        return new SimpleMessageKey(path, "");
    }

    /**
     * Creates a child message key with empty default message
     */
    public static SimpleMessageKey of(String path, MessageKey parent) {
        return new SimpleMessageKey(path, "", parent);
    }



    private static String buildPath(MessageKey parent, String path) {
        return parent != null ? parent.getPath() + "." + path : path;
    }
}
