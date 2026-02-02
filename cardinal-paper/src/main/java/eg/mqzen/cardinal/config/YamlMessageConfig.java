package eg.mqzen.cardinal.config;

import eg.mqzen.lib.config.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.config.MessageKey;
import org.bukkit.command.CommandSender;
import java.io.IOException;
import java.util.Objects;

/**
 * Main implementation of the MessageConfig interface
 */
public class YamlMessageConfig implements MessageConfig {
    private final YamlDocument document;
    private final MiniMessage miniMessage;
    private final TagResolver primaryColor, secondaryColor;
    public YamlMessageConfig(YamlDocument document) {
        this.document = Objects.requireNonNull(document, "YamlDocument cannot be null");

        primaryColor = Placeholder.styling("pc", (styleBuilder)-> {
            String MM = getRawMessage(MessageKeys.PRIMARY_COLOR);
            Component primaryComp = MiniMessage.miniMessage().deserialize(MM);
            styleBuilder.merge(primaryComp.style());
        } );

        secondaryColor = Placeholder.styling("sc", (styleBuilder)-> {
            String MM = getRawMessage(MessageKeys.SECONDARY_COLOR);
            Component secondaryComp = MiniMessage.miniMessage().deserialize(MM );
            styleBuilder.merge(secondaryComp.style());
        } );


        this.miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(TagResolver.standard())
                        .resolver(primaryColor)
                        .resolver(secondaryColor)
                        .build())
                .build();


    }

    @Override
    public void sendRichMessage(CommandSender sender, MessageKey key, TagResolver... resolvers) {
        sender.sendMessage(getMessage(key, resolvers));
    }

    @Override
    public Component getMessage(MessageKey key, TagResolver... resolvers) {
        String rawMessage = getRawMessage(key);
        if(rawMessage == null) {
            System.out.println("RAW IS NULL");
            return Component.empty();
        }

        if(resolvers == null) {
            System.out.println("RESOLVERS ARE NULL");
            return Component.empty();
        }

        TagResolver prefixResolver = TagResolver.builder()
                .resolvers(primaryColor, secondaryColor)
                .tag("prefix", Tag.preProcessParsed(getRawMessage(MessageKeys.PREFIX)))
                .build();

        TagResolver allResolvers = TagResolver.builder()
                .resolver(primaryColor)
                .resolver(secondaryColor)
                .resolver(prefixResolver)
                .resolvers(resolvers)
                .build();

        if (resolvers.length == 0) {
            return Component.empty();
        }

        // Use MiniMessage to deserialize with tag resolvers, then serialize back to string
        return miniMessage.deserialize(rawMessage, allResolvers);
    }

    @Override
    public String getRawMessage(MessageKey key) {
        Objects.requireNonNull(key, "MessageKey cannot be null");

        String message = document.getString(key.getPath());
        if (message == null || message.trim().isEmpty()) {
            return key.getDefaultMessage();
        }
        return message;
    }

    @Override
    public boolean hasMessage(MessageKey key) {
        Objects.requireNonNull(key, "MessageKey cannot be null");
        return document.contains(key.getPath()) &&
               document.getString(key.getPath()) != null;
    }

    @Override
    public void reload() {
        // Assuming boosted-yaml has a reload method
        try {
            document.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public YamlDocument getDocument() {
        return document;
    }
}