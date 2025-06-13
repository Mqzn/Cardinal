package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Optional;
import com.mineabyss.lib.commands.annotations.Usage;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.commands.api.CardinalSource;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Command("unmute")
public final class UnMuteCommand {

    @Usage
    public void def(CardinalSource source) {
        source.sendMsg("<red>Unmute <user> [reason...]");
    }

    @Usage
    public void unmute(
            PunishmentIssuer issuer,
            @Named("user")OfflinePlayer user,
            @Named("reason") @Greedy @Optional String reason
    ) {

        if(user.getName() == null) {
            issuer.sendMsg("<red>User doesn't exist !");
            return;
        }

        UUID userUUID = user.getUniqueId();
        Cardinal.getInstance().getPunishmentManager()
                .getActivePunishment(userUUID, StandardPunishmentType.MUTE)
                .thenCompose((punishmentContainer)-> {

                    if(punishmentContainer.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }else {

                        Punishment<?> punishment = punishmentContainer.get();
                        return Cardinal.getInstance().getPunishmentManager()
                                .revokePunishment(punishment.getId(), issuer, reason).unwrap();
                    }

                })
                .onSuccess((revoked)-> {

                    if(revoked) {
                        //send success to user
                        issuer.sendMsg("<gray>Unmuted player <green>" + user.getName());
                    }
                    else {
                        issuer.sendMsg("<dark_red>ERROR: <red>Player '" + user.getName() + "' is not muted !");
                    }
                });

    }



}
