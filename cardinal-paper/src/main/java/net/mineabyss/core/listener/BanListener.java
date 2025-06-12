package net.mineabyss.core.listener;

import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.punishments.issuer.PunishmentIssuerFactory;
import net.mineabyss.core.util.PunishmentMessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import java.util.UUID;


public class BanListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(AsyncPlayerPreLoginEvent event) {

        //String userName = event.getName();
        UUID uuid = event.getUniqueId();

        var punishmentContainer = Cardinal.getInstance().getPunishmentManager()
                .getActivePunishment(uuid, StandardPunishmentType.BAN)
                .join();

        if(punishmentContainer.isEmpty()) {
            return;
        }

        Punishment<?> punishment = punishmentContainer.get();
        if(!punishment.isPermanent() && punishment.hasExpired()) {
            //we remove from memory
            boolean revoked = Cardinal.getInstance().getPunishmentManager().revokePunishment(punishment.getId(),
                    PunishmentIssuerFactory.fromConsole(), "EXPIRED").join();
            if(revoked) {
                event.allow();
                return;
            }
            return;
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, PunishmentMessageUtil.getKickMessage(punishment));
    }

}
