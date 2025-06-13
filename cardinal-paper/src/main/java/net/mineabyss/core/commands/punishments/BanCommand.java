package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Default;
import com.mineabyss.lib.commands.annotations.Description;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Optional;
import com.mineabyss.lib.commands.annotations.Permission;
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
import net.mineabyss.core.util.Tasks;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Command("ban")
@Permission("cardinal.command.ban")
@Description("Bans a player from the server.")
public class BanCommand {

    @Usage
    public void defaultUsage(CardinalSource sender) {
        sender.sendMsg("Usage: /ban <player> [-s] [duration] [reason...]");
    }


    @Usage
    public void banPlayer(
            PunishmentIssuer issuer,
            @Named("player") OfflinePlayer player,
            @Switch({"silent", "s"}) boolean silent,
            @Named("duration")@Default("0") @Nullable Duration duration,
            @Named("reason") @Greedy @Optional @Nullable String reason
    ) {
        // /ban <player> [-s] [duration] [reason]

        //check if the user is already banned
        Cardinal.getInstance()
                .getPunishmentManager().getActivePunishment(player.getUniqueId(), StandardPunishmentType.BAN)
                .unwrap().thenCompose((punishmentContainer)-> {
                            if(punishmentContainer.isPresent()) {

                                if(issuer.hasPermission(CardinalPermissions.OVERRIDE_PUNISHMENTS_PERMISSION)) {
                                    //let's override.
                                    Punishment<?> punishment = punishmentContainer.get();
                                    punishment.setReason(reason);
                                    punishment.setDuration(duration);
                                    return Cardinal.getInstance().getPunishmentManager().applyPunishment(punishment)
                                    .unwrap().<java.util.Optional<Punishment<?>>>thenApply(java.util.Optional::of);
                                }else {
                                    //send that he's already banned
                                    issuer.sendMsg("<dark_red>ERROR:</dark_red> <red>User '" + player.getName() + "' is already banned !");
                                    return CompletableFuture.completedFuture(punishmentContainer);
                                }

                            }else {
                                return Cardinal.getInstance().getPunishmentManager()
                                        .applyPunishment(StandardPunishmentType.BAN, issuer, PunishmentTargetFactory.playerTarget(player), duration, reason)
                                        .map((p)-> (Punishment<?>)p)
                                        .onSuccess((punishment)-> {
                                            //kick if online
                                            if(player.isOnline()) {
                                                //TODO make a punishment message manager, fetch (kick) messages for each punishment type
                                                Tasks.runSync(()-> {
                                                    ((Player)player).kick(PunishmentMessageUtil.getKickMessage(punishment),
                                                            PlayerKickEvent.Cause.BANNED);
                                                });
                                            }
                                            PunishmentMessageUtil.broadcastPunishment(punishment, silent);
                                        }).unwrap()
                                        .thenApply(java.util.Optional::of);
                            }

                        });

    }

}
