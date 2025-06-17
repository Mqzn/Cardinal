package net.mineabyss.core.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.util.PunishmentMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MuteListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        var punishmentContainer = Cardinal.getInstance().getPunishmentManager().getActivePunishment(player.getUniqueId(), StandardPunishmentType.MUTE)
                        .join();

        if(punishmentContainer.isPresent()) {
            Punishment<?> punishment = punishmentContainer.get();
            player.sendMessage(PunishmentMessageUtil.getMuteChatBlock(punishment));
            event.setCancelled(true);
        }


    }

}
