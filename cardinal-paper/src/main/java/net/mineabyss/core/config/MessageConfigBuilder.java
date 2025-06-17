package net.mineabyss.core.config;

import com.mineabyss.lib.config.YamlDocument;
import net.mineabyss.cardinal.api.config.MessageConfig;

import java.util.Objects;

/**
 * Builder for creating MessageConfig instances with fluent API
 */
public class MessageConfigBuilder {
    private YamlDocument document;
    
    public MessageConfigBuilder document(YamlDocument document) {
        this.document = document;
        return this;
    }
    
    public MessageConfig build() {
        Objects.requireNonNull(document, "YamlDocument must be provided");
        return new YamlMessageConfig(document);
    }
    
    public static MessageConfigBuilder create() {
        return new MessageConfigBuilder();
    }
}