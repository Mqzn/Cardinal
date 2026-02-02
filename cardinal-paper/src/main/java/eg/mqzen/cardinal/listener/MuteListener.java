package eg.mqzen.cardinal.listener;

import eg.mqzen.cardinal.Cardinal;
import io.papermc.paper.event.player.AsyncChatEvent;
import eg.mqzen.cardinal.api.CardinalProvider;
import eg.mqzen.cardinal.api.punishments.PunishmentScanResult;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.List;
import java.util.Objects;

public class MuteListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        String ipAddress = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();

        PunishmentScanResult result = CardinalProvider.provide().getPunishmentManager()
                .scan(player.getUniqueId(), ipAddress, StandardPunishmentType.MUTE)
                .join();

        if(result.failed()) {
            result.log();
        }

        result.getFoundPunishment().ifPresent((punishment)-> {
            player.sendMessage(PunishmentMessageUtil.getMuteChatBlock(punishment));
            event.setCancelled(true);
        });

    }

    @EventHandler
    public void onCommandExecution(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        String msg = event.getMessage();
        List<String> blockedCommands = Objects.requireNonNull(Cardinal.getInstance().getConfigYaml())
                .getStringList("mute-blocked-commands");

        if(blockedCommands.stream().anyMatch(msg::startsWith)) {
            String ipAddress = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();

            PunishmentScanResult result = CardinalProvider.provide().getPunishmentManager()
                    .scan(player.getUniqueId(), ipAddress, StandardPunishmentType.MUTE)
                    .join();

            if(result.failed()) {
                result.log();
                return;
            }

            result.getFoundPunishment()
                    .ifPresent((punishment)-> {
                        player.sendMessage(PunishmentMessageUtil.getMuteChatBlock(punishment));
                        event.setCancelled(true);
                    });
        }

    }

}
