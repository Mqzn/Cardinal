package net.mineabyss.core.punishments.target;

import net.mineabyss.cardinal.api.punishments.Punishable;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class PunishmentTargetFactory {

    public static Punishable<UUID> playerTarget(UUID uuid, String name) {
        return new PlayerTarget(uuid, name);
    }

    public static Punishable<UUID> playerTarget(OfflinePlayer offlinePlayer) {
        return playerTarget(offlinePlayer.getUniqueId(), offlinePlayer.getName());
    }

    public static Punishable<String> playerIPTarget(UUID uuid, String name, String ipAddress) {
        return new IPTarget(new PlayerTarget(uuid, name), ipAddress);
    }

    public static Punishable<String> ipTarget(String ipAddress) {
        return new IPTarget(null, ipAddress);
    }

}
