package eg.mqzen.cardinal.commands.punishments;

import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.CardinalPermissions;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.config.MessageKey;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.commands.api.DefaultReasonProvider;
import eg.mqzen.cardinal.config.MessageKeys;
import eg.mqzen.cardinal.punishments.target.PunishmentTargetFactory;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;
import eg.mqzen.cardinal.util.Tasks;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Dependency;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Usage;

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
        if(silent && !issuer.hasPermission(CardinalPermissions.USE_SILENT_FLAG_PERMISSION)) {
            issuer.sendMsg("<red>You do not have permission to use the silent flag!");
            return;
        }
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
