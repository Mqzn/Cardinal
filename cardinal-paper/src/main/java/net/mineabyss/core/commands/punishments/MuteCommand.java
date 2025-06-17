package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.DefaultProvider;
import com.mineabyss.lib.commands.annotations.Dependency;
import com.mineabyss.lib.commands.annotations.Description;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Optional;
import com.mineabyss.lib.commands.annotations.Permission;
import com.mineabyss.lib.commands.annotations.Switch;
import com.mineabyss.lib.commands.annotations.Usage;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import static net.mineabyss.core.config.MessageKeys.Punishments.Mute;

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
            @Named("user") Punishable<?> target,
            @Switch("silent") boolean silent,
            @Named("duration") @Optional Duration duration,
            @Named("reason") @DefaultProvider(DefaultReasonProvider.class) String reason
    ) {

        target.fetchPunishment(StandardPunishmentType.MUTE)
                .thenCompose((punishmentContainer)-> {
                    if(punishmentContainer.isPresent()) {

                        if(issuer.hasPermission(CardinalPermissions.OVERRIDE_PUNISHMENTS_PERMISSION)) {
                            Punishment<?> punishment = punishmentContainer.get();
                            punishment.setDuration(duration);
                            punishment.setReason(reason);
                            return Cardinal.getInstance().getPunishmentManager()
                                    .applyPunishment(punishment)
                                    .unwrap().<java.util.Optional<Punishment<?>>>thenApply(java.util.Optional::of);
                        }else {
                            //no perm
                            issuer.sendMsg("<dark_red>ERROR:</dark_red> <red>User '" + target.getTargetName() + "' is already muted !");
                            return CompletableFuture.completedFuture(punishmentContainer);
                        }

                    }else {
                        return Cardinal.getInstance().getPunishmentManager()
                                .applyPunishment(StandardPunishmentType.MUTE, issuer, target, duration, reason)
                                .map((p)-> (Punishment<?>)p)
                                .onSuccess((punishment)-> {
                                    //kick if online
                                    OfflinePlayer offlinePlayer = target.asOfflinePlayer();
                                    if(offlinePlayer != null && offlinePlayer.isOnline()) {
                                        MessageKey key = punishment.isPermanent() ?  Mute.NOTIFICATION_PERMANENT : Mute.NOTIFICATION_TEMPORARY;
                                        ((Player)offlinePlayer).sendMessage(config.getMessage(key, punishment.asTagResolver()));
                                    }
                                    MessageKey normalBCKey = punishment.isPermanent() ? Mute.BROADCAST : Mute.BROADCAST_TEMPORARY;
                                    MessageKey silentBCKey = punishment.isPermanent() ? Mute.BROADCAST_SILENT : Mute.BROADCAST_TEMPORARY_SILENT;

                                    PunishmentMessageUtil.broadcastPunishment(normalBCKey, silentBCKey, punishment, silent);

                                    //send success
                                    issuer.sendMsg(config.getMessage(punishment.isPermanent() ? Mute.SUCCESS : Mute.SUCCESS_TEMPORARY, punishment.asTagResolver()));
                                }).unwrap()
                                .thenApply(java.util.Optional::of);
                    }

                });


    }



}
