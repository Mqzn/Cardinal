package eg.mqzen.cardinal.commands.punishments;

import eg.mqzen.lib.commands.annotations.Command;
import eg.mqzen.lib.commands.annotations.DefaultProvider;
import eg.mqzen.lib.commands.annotations.Dependency;
import eg.mqzen.lib.commands.annotations.Description;
import eg.mqzen.lib.commands.annotations.Greedy;
import eg.mqzen.lib.commands.annotations.Named;
import eg.mqzen.lib.commands.annotations.Permission;
import eg.mqzen.lib.commands.annotations.Switch;
import eg.mqzen.lib.commands.annotations.Usage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.config.MessageKey;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.CardinalPermissions;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.commands.api.DefaultReasonProvider;
import eg.mqzen.cardinal.config.MessageKeys;
import eg.mqzen.cardinal.punishments.target.PunishmentTargetFactory;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;
import eg.mqzen.cardinal.util.Tasks;
import org.bukkit.entity.Player;
import java.time.Duration;


@Command("kick")
@Permission(CardinalPermissions.KICK_COMMAND_PERMISSION)
@Description("Kicks a player from the server.")
public class KickCommand {

    @Dependency
    private MessageConfig config;

    @Usage
    public void def(CardinalSource source) {
        source.sendMsg("<red>/kick <user> [-s] [reason]");
    }

    @Usage
    public void exec(
            PunishmentIssuer issuer,
            @Named("user") Player player,
            @Switch("silent") boolean silent,
            @Named("reason") @DefaultProvider(DefaultReasonProvider.class) @Greedy String reason
    ) {
        Cardinal.getInstance().getPunishmentManager()
                .applyPunishment(StandardPunishmentType.KICK, issuer, PunishmentTargetFactory.playerTarget(player), Duration.ZERO, reason)
                .onSuccess((punishment)-> {
                    TagResolver punishmentTags = punishment.asTagResolver();

                    Tasks.runSync(()-> {
                        player.kick(config.getMessage(MessageKeys.Punishments.Kick.MESSAGE, punishmentTags));
                    });

                    MessageKey normalKey = MessageKeys.Punishments.Kick.BROADCAST;
                    MessageKey silentKey = MessageKeys.Punishments.Kick.BROADCAST_SILENT;
                    PunishmentMessageUtil.broadcastPunishment(normalKey, silentKey, punishment, silent);

                    issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Kick.SUCCESS, punishmentTags));
                });

    }
}
