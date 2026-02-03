package eg.mqzen.cardinal.commands.punishments;

import static eg.mqzen.cardinal.config.MessageKeys.Punishments.Ban;

import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.CardinalPermissions;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.config.MessageKey;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.commands.api.DefaultReasonProvider;
import eg.mqzen.cardinal.util.Pair;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;
import eg.mqzen.cardinal.util.Tasks;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Dependency;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Usage;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

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
        if(silent && !issuer.hasPermission(CardinalPermissions.USE_SILENT_FLAG_PERMISSION)) {
            issuer.sendMsg("<red>You do not have permission to use the silent flag!");
            return;
        }

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
