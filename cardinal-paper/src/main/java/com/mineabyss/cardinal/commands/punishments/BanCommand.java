package com.mineabyss.cardinal.commands.punishments;

import com.mineabyss.cardinal.util.Pair;
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
import com.mineabyss.cardinal.api.config.MessageConfig;
import com.mineabyss.cardinal.api.config.MessageKey;
import com.mineabyss.cardinal.api.punishments.Punishable;
import com.mineabyss.cardinal.api.punishments.Punishment;
import com.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import com.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import com.mineabyss.cardinal.Cardinal;
import com.mineabyss.cardinal.CardinalPermissions;
import com.mineabyss.cardinal.commands.api.CardinalSource;
import com.mineabyss.cardinal.commands.api.DefaultReasonProvider;
import com.mineabyss.cardinal.util.PunishmentMessageUtil;
import com.mineabyss.cardinal.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.mineabyss.cardinal.config.MessageKeys.Punishments.Ban;

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
            @Named("player") CompletableFuture<Punishable<?>> targetFuture,
            @Switch({"silent", "s"}) boolean silent,
            @Named("duration") @Optional Duration duration,
            @Named("reason") @Greedy @DefaultProvider(DefaultReasonProvider.class) @NotNull String reason
    ) {
        // /ban <player> [-s] [duration] [reason]

        // Use thenCombine to combine target and punishment container without Pair
        targetFuture.thenCombine(
                targetFuture.thenCompose(target ->
                        target.fetchPunishment(StandardPunishmentType.BAN)
                                .onError(Throwable::printStackTrace)
                                .unwrap()
                ),
                (target, punishmentContainer) -> {
                    // Process the ban logic with both target and punishmentContainer
                    if (punishmentContainer.isPresent()) {
                        if (issuer.hasPermission(CardinalPermissions.OVERRIDE_PUNISHMENTS_PERMISSION)) {
                            // Override existing punishment
                            Punishment<?> punishment = punishmentContainer.get();
                            punishment.setReason(reason);
                            punishment.setDuration(duration);
                            return Cardinal.getInstance().getPunishmentManager()
                                    .applyPunishment(punishment)
                                    .unwrap()
                                    .thenCombine(CompletableFuture.completedFuture(target),
                                            (p, t) -> new Pair<>(t, p))
                                    .join();
                        } else {
                            // Send already banned message
                            issuer.sendMsg(config.getMessage(Ban.ALREADY_BANNED,
                                    Placeholder.unparsed("target", target.getTargetName())));
                            return new Pair<>(target, punishmentContainer.get());
                        }
                    } else {
                        // Apply new punishment
                        Cardinal.log("Applying new punishment to " + target.getTargetName());
                        return Cardinal.getInstance().getPunishmentManager()
                                .applyPunishment(StandardPunishmentType.BAN, issuer, target, duration, reason)
                                .map(p -> (Punishment<?>) p)
                                .unwrap()
                                .thenCombine(CompletableFuture.completedFuture(target),
                                        (p, t) -> new Pair<>(t, p))
                                .join();
                    }
                }
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            }

            var punishment = result.right();
            var target = result.left();

            // Kick if online
            Player online = Bukkit.getPlayer(target.getTargetUUID());
            if (online != null && online.isOnline()) {
                Tasks.runSync(() -> {
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
