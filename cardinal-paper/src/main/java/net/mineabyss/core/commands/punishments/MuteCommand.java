package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Optional;
import com.mineabyss.lib.commands.annotations.Switch;
import com.mineabyss.lib.commands.annotations.Usage;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.CardinalPermissions;
import net.mineabyss.core.commands.api.CardinalSource;
import net.mineabyss.core.punishments.target.PunishmentTargetFactory;
import net.mineabyss.core.util.PunishmentMessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Command("mute")
public class MuteCommand {

    @Usage
    public void exec(CardinalSource source) {
        source.sendMsg("<red>/mute <user> [-s] [duration] [reason]");
    }

    @Usage
    public void exec(
            PunishmentIssuer issuer,
            @Named("user") OfflinePlayer offlinePlayer,
            @Switch("silent") boolean silent,
            @Named("duration") @Optional Duration duration,
            @Named("reason") @Optional String reason
    ) {


        Cardinal.getInstance()
                .getPunishmentManager()
                .getActivePunishment(offlinePlayer.getUniqueId(), StandardPunishmentType.MUTE)
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
                            issuer.sendMsg("<dark_red>ERROR:</dark_red> <red>User '" + offlinePlayer.getName() + "' is already muted !");
                            return CompletableFuture.completedFuture(punishmentContainer);
                        }

                    }else {
                        return Cardinal.getInstance().getPunishmentManager()
                                .applyPunishment(StandardPunishmentType.BAN, issuer, PunishmentTargetFactory.playerTarget(offlinePlayer), duration, reason)
                                .map((p)-> (Punishment<?>)p)
                                .onSuccess((punishment)-> {
                                    //kick if online
                                    if(offlinePlayer.isOnline()) {
                                        //TODO make a punishment message manager, fetch (kick) messages for each punishment type
                                        ((Player)offlinePlayer).sendRichMessage(PunishmentMessageUtil.getMuteMessageMiniMessage(punishment));
                                    }
                                    PunishmentMessageUtil.broadcastPunishment(punishment, silent);
                                }).unwrap()
                                .thenApply(java.util.Optional::of);
                    }

                });


    }



}
