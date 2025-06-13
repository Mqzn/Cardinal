package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Usage;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.commands.api.CardinalSource;
import org.bukkit.OfflinePlayer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Command("unban")
public class UnbanCommand {

    @Usage
    public void def(CardinalSource source) {
        // /unban
        source.sendMsg("<red>/Unban <user> [reason]");
    }

    @Usage
    public void exec(PunishmentIssuer issuer, @Named("user")OfflinePlayer user, @Named("reason") String reason) {

        if(user.getName() == null) {
            issuer.sendMsg("<red>User doesn't exist !");
            return;
        }

        UUID userUUID = user.getUniqueId();
        Cardinal.getInstance().getPunishmentManager()
                .getActivePunishment(userUUID, StandardPunishmentType.BAN)
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
                        issuer.sendMsg("<gray>Unbanned player <green>" + user.getName());
                    }
                    else {
                        issuer.sendMsg("<dark_red>ERROR: <red>Player '" + user.getName() + "' is not banned !");
                    }
                });

    }


}
