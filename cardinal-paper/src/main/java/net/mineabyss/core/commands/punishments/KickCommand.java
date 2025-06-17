package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.DefaultProvider;
import com.mineabyss.lib.commands.annotations.Dependency;
import com.mineabyss.lib.commands.annotations.Description;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Permission;
import com.mineabyss.lib.commands.annotations.Switch;
import com.mineabyss.lib.commands.annotations.Usage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.mineabyss.cardinal.api.config.MessageConfig;
import net.mineabyss.cardinal.api.config.MessageKey;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.CardinalPermissions;
import net.mineabyss.core.commands.api.CardinalSource;
import net.mineabyss.core.commands.api.DefaultReasonProvider;
import net.mineabyss.core.config.MessageKeys;
import net.mineabyss.core.punishments.target.PunishmentTargetFactory;
import net.mineabyss.core.util.PunishmentMessageUtil;
import net.mineabyss.core.util.Tasks;
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
