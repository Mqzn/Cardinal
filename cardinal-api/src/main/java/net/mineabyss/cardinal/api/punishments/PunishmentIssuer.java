package net.mineabyss.cardinal.api.punishments;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an issuer of a punishment.
 * This interface provides methods to retrieve the name, unique identifier, and type of the issuer.
 */
public interface PunishmentIssuer {

    /**
     * Returns the name of the issuer.
     * @return the name of the issuer
     */
    @NotNull String getName();

    /**
     * Returns the unique identifier for this issuer.
     * @return the unique identifier as a UUID
     */
    @NotNull UUID getUniqueId();


    /**
     * Returns the type of this issuer.
     * @return the type of the issuer
     */
    @NotNull IssuerType getType();

    /**
     * Checks if issuer has specific permission
     * @param permission the specific permission
     * @return whether the issuer has a specific permission
     */
    boolean hasPermission(String permission);

    /**
     * Checks if the issuer is a console.
     * @return true if the issuer is a console, false otherwise
     */
    default boolean isConsole() {
        return getType().isConsole();
    }

    /**
     * Checks if the issuer is a player.
     * @return true if the issuer is a player, false otherwise
     */
    default boolean isPlayer() {
        return getType().isPlayer();
    }

    void sendMsg(String msg);

    void sendMsg(Component component);
}