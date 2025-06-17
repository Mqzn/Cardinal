package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.DefaultProvider;
import com.mineabyss.lib.commands.annotations.Dependency;
import com.mineabyss.lib.commands.annotations.Description;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Optional;
import com.mineabyss.lib.commands.annotations.Permission;
import com.mineabyss.lib.commands.annotations.Switch;
import com.mineabyss.lib.commands.annotations.Usage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.mineabyss.cardinal.api.config.MessageConfig;
import net.mineabyss.cardinal.api.config.MessageKey;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.CardinalPermissions;
import net.mineabyss.core.commands.api.CardinalSource;
import net.mineabyss.core.commands.api.DefaultReasonProvider;
import net.mineabyss.core.util.PunishmentMessageUtil;
import net.mineabyss.core.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import static net.mineabyss.core.config.MessageKeys.Punishments.Ban;

@Command("ban")
@Permission(CardinalPermissions.BAN_COMMAND_PERMISSION)
@Description("Bans a player from the server.")
public class BanCommand {

    @Dependency
    private MessageConfig config;

    @Usage
    public void defaultUsage(CardinalSource sender) {
        sender.sendMsg("Usage: /ban <player> [-s] [duration] [reason...]");
    }


    @Usage
    public void banPlayer(
            PunishmentIssuer issuer,
            @Named("player") Punishable<?> target,
            @Switch({"silent", "s"}) boolean silent,
            @Named("duration") @Optional Duration duration,
            @Named("reason") @Greedy @DefaultProvider(DefaultReasonProvider.class) @NotNull String reason
    ) {
        // /ban <player> [-s] [duration] [reason]

        //check if the user is already banned
        target.fetchPunishment(StandardPunishmentType.BAN)
                .onError(Throwable::printStackTrace)
                .thenApplyAsync((punishmentContainer)-> {
                    if(punishmentContainer.isPresent()) {

                        if(issuer.hasPermission(CardinalPermissions.OVERRIDE_PUNISHMENTS_PERMISSION)) {
                            //let's override.
                            Punishment<?> punishment = punishmentContainer.get();
                            punishment.setReason(reason);
                            punishment.setDuration(duration);
                            return Cardinal.getInstance().getPunishmentManager()
                                    .applyPunishment(punishment)
                                    .join();
                        }else {
                            //send that he's already banned
                            issuer.sendMsg(config.getMessage(Ban.ALREADY_BANNED, Placeholder.unparsed("target", target.getTargetName())));
                            return punishmentContainer.get();
                        }

                    }else {
                        Cardinal.log("Applying new punishment to " + target.getTargetName());
                        return Cardinal.getInstance().getPunishmentManager()
                                .applyPunishment(StandardPunishmentType.BAN, issuer, target, duration, reason)
                                .map((p)-> (Punishment<?>)p)
                                .join();
                    }

                })
                .onSuccess((punishment)-> {

                    //kick if online
                    Player online = Bukkit.getPlayer(target.getTargetUUID());
                    if (online != null && online.isOnline()) {
                        Tasks.runSync(()-> {
                            online.kick(
                                    config.getMessage(punishment.isPermanent() ? Ban.KICK_MESSAGE_PERMANENT : Ban.KICK_MESSAGE_TEMPORARY,
                                            punishment.asTagResolver()),
                                    PlayerKickEvent.Cause.BANNED
                            );
                        });
                    }

                    MessageKey normalKey = punishment.isPermanent() ? Ban.BROADCAST : Ban.BROADCAST_TEMPORARY;
                    MessageKey silentKey = punishment.isPermanent() ? Ban.BROADCAST_SILENT : Ban.BROADCAST_TEMPORARY_SILENT;

                    PunishmentMessageUtil.broadcastPunishment(normalKey, silentKey, punishment, silent);

                    issuer.sendMsg(config.getMessage(punishment.isPermanent() ? Ban.SUCCESS : Ban.SUCCESS_TEMPORARY, punishment.asTagResolver()));
                });

    }

}
