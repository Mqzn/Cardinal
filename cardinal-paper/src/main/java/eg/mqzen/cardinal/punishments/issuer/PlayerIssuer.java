package eg.mqzen.cardinal.punishments.issuer;

import net.kyori.adventure.text.Component;
import eg.mqzen.cardinal.api.punishments.IssuerType;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerIssuer implements PunishmentIssuer {

    private final String name;
    private final UUID uniqueId;

    PlayerIssuer (@NotNull Player player) {
        this.name = player.getName();
        this.uniqueId = player.getUniqueId();
    }

    PlayerIssuer (@NotNull String name, UUID uuid) {
        this.name = name;
        this.uniqueId = uuid;
    }


    /**
     * Returns the name of the issuer.
     *
     * @return the name of the issuer
     */
    @NotNull @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the unique identifier for this issuer.
     *
     * @return the unique identifier as a UUID
     */
    @Override
    public @NotNull UUID getUniqueId() {
        return uniqueId;
    }


    /**
     * Returns the type of this issuer.
     *
     * @return the type of the issuer
     */
    @Override
    public @NotNull IssuerType getType() {
        return IssuerType.PLAYER;
    }

    @Override
    public boolean hasPermission(String permission) {
        Player player = Bukkit.getPlayer(uniqueId);
        if(player == null) {
            return false;
        }
        return player.hasPermission(permission);

    }

    @Override
    public void sendMsg(String msg) {
        Player player = Bukkit.getPlayer(uniqueId);
        if(player != null) {
            player.sendRichMessage(msg);
        }
    }

    @Override
    public void sendMsg(Component component) {
        Player player = Bukkit.getPlayer(uniqueId);
        if(player != null) {
            player.sendMessage(component);
        }
    }


}
