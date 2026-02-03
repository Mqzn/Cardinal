package eg.mqzen.cardinal.commands.punishments;

import static eg.mqzen.cardinal.config.MessageKeys.Punishments.Mute;

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
import eg.mqzen.cardinal.config.MessageKeys;
import eg.mqzen.cardinal.util.Pair;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Dependency;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Usage;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Command("mute")
@Permission(CardinalPermissions.MUTE_COMMAND_PERMISSION)
@Description("Mutes a player on the server.")
public class MuteCommand {

    @Dependency
    private MessageConfig config;

    @Usage
    public void exec(CardinalSource source) {
        source.sendMsg("<red>/mute <user> [-s] [duration] [reason]");
    }

    @Usage
    public void exec(
            PunishmentIssuer issuer,
            @Named("user") CompletableFuture<Punishable<?>> targetFuture,
            @Switch("silent") boolean silent,
            @Named("duration") @Optional Duration duration,
            @Named("reason") @DefaultProvider(DefaultReasonProvider.class) String reason
    ) {
        if(!issuer.hasPermission(CardinalPermissions.USE_SILENT_FLAG_PERMISSION)) {
            issuer.sendMsg("<red>You do not have permission to use the silent flag!");
            return;
        }
        targetFuture.thenCompose((target)-> {
            return target.fetchPunishment(StandardPunishmentType.MUTE)
                    .onError(Throwable::printStackTrace)
                    .thenApply((punishmentContainer)-> new Pair<>(target, punishmentContainer)).unwrap();
        }).thenApplyAsync((data)-> {

            Punishable<?> target = data.left();
            var punishmentContainer = data.right();

            if(punishmentContainer.isPresent()) {

                if(issuer.hasPermission(CardinalPermissions.OVERRIDE_PUNISHMENTS_PERMISSION)) {
                    //let's override.
                    Punishment<?> punishment = punishmentContainer.get();
                    punishment.setReason(reason);
                    punishment.setDuration(duration);
                    return Cardinal.getInstance().getPunishmentManager()
                            .applyPunishment(punishment)
                            .thenApply((p)-> new Pair<>(target, p))
                            .join();
                }else {
                    //send that he's already muted
                    issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Mute.ALREADY_MUTED, Placeholder.unparsed("target",
                            target.getTargetName())));
                    return new Pair<>(target, punishmentContainer.get());
                }

            }else {
                Cardinal.log("Applying new punishment to " + target.getTargetName());
                return Cardinal.getInstance().getPunishmentManager()
                        .applyPunishment(StandardPunishmentType.MUTE, issuer, target, duration, reason)
                        .map((p)-> (Punishment<?>)p)
                        .thenApply((p)-> new Pair<>(target, p))
                        .join();
            }

        }).whenComplete( (data, ex) -> {
            if(ex != null ) {
               ex.printStackTrace();
               return;
            }

            Punishable<?> target = data.left();
            Punishment<?> punishment = data.right();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target.getTargetUUID());
            if(offlinePlayer.isOnline()) {
                MessageKey key = punishment.isPermanent() ?  Mute.NOTIFICATION_PERMANENT : Mute.NOTIFICATION_TEMPORARY;
                ((Player)offlinePlayer).sendMessage(config.getMessage(key, punishment.asTagResolver()));
            }
            MessageKey normalBCKey = punishment.isPermanent() ? Mute.BROADCAST : Mute.BROADCAST_TEMPORARY;
            MessageKey silentBCKey = punishment.isPermanent() ? Mute.BROADCAST_SILENT : Mute.BROADCAST_TEMPORARY_SILENT;

            PunishmentMessageUtil.broadcastPunishment(normalBCKey, silentBCKey, punishment, silent);

            //send success
            issuer.sendMsg(config.getMessage(punishment.isPermanent() ? Mute.SUCCESS : Mute.SUCCESS_TEMPORARY, punishment.asTagResolver()));

        });
    }



}
