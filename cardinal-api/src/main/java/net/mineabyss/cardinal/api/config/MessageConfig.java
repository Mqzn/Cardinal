package net.mineabyss.cardinal.api.config;// Using Kyori Adventure's TagResolver
import com.mineabyss.lib.config.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/**
 * Main interface for the message configuration API
 */
public interface MessageConfig {

    void sendRichMessage(CommandSender sender, MessageKey key, TagResolver... resolvers);

    /**
     * Fetches a message by its key with optional tag resolvers
     */
    Component getMessage(MessageKey key, TagResolver... resolvers);

    /**
     * Fetches a raw message without placeholder resolution
     */
    String getRawMessage(MessageKey key);
    
    /**
     * Checks if a message key exists in the configuration
     */
    boolean hasMessage(MessageKey key);
    
    /**
     * Reloads the configuration from the underlying document
     */
    void reload();
    
    /**
     * Gets the underlying YAML document
     */
    YamlDocument getDocument();
}
