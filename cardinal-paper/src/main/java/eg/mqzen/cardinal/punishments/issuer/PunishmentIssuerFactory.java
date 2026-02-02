package eg.mqzen.cardinal.punishments.issuer;

import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;


public final class PunishmentIssuerFactory {

    public static PunishmentIssuer fromPlayer(Player player) {
        return new PlayerIssuer(player);
    }


    public static PunishmentIssuer fromPlayer(@NotNull OfflinePlayer player) {
        return new PlayerIssuer(Objects.requireNonNull(player.getName()), player.getUniqueId());
    }

    public static PunishmentIssuer fromPlayerInfo(UUID uuid, String name) {
        return new PlayerIssuer(name, uuid);
    }

    public static PunishmentIssuer fromConsole() {
        return ConsoleIssuer.get();
    }

    public static PunishmentIssuer fromUUID(UUID revoker) {
        if(revoker == null)return fromConsole();
        else return fromPlayer(Bukkit.getOfflinePlayer(revoker));
    }
}
