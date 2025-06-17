package net.mineabyss.core.punishments.target;

import net.kyori.adventure.text.Component;
import net.mineabyss.cardinal.api.CardinalProvider;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.PunishableType;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.util.FutureOperation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.time.Instant;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

final class PlayerTarget implements Punishable<UUID> {

    private final UUID playerUUID;
    private final String playerName;

    private Instant lastSeen;

    /**
     * Constructs a PlayerTarget with the specified UUID and name.
     *
     * @param playerUUID the UUID of the player
     * @param playerName the name of the player
     */
    PlayerTarget(@NotNull UUID playerUUID, @NotNull String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.lastSeen = Instant.now(); // Initialize last seen to current time
    }

    /**
     * Returns the unique identifier for this punishable entity.
     *
     * @return the unique identifier
     */
    @Override
    public @NotNull PunishableType getType() {
        return PunishableType.PLAYER;
    }

    /**
     * Returns the name of the target entity.
     *
     * @return the name of the target entity
     */
    @NotNull
    @Override
    public String getTargetName() {
        return playerName;
    }

    /**
     * Returns the UUID of the target entity.
     *
     * @return the UUID of the target entity
     */
    @NotNull
    @Override
    public UUID getTargetUUID() {
        return playerUUID;
    }

    /**
     * Returns the target entity itself.
     *
     * @return the target entity
     */
    @NotNull
    @Override
    public UUID getTarget() {
        return playerUUID;
    }

    /**
     * Returns the last seen time of the target entity.
     *
     * @return the last seen time as an Instant
     */
    @Override
    public Instant getLastSeen() {
        return lastSeen;
    }

    /**
     * Sets last seen time of the target entity to the current time.
     */
    @Override
    public void refreshLastSeen() {
        this.lastSeen = Instant.now();
    }

    @Override
    public void kick(Component component) {
        Player player = Bukkit.getPlayer(playerUUID);
        if(player != null) {
            player.kick(component);
        }
    }

    @Override
    public void sendMsg(String msg) {
        Player player = Bukkit.getPlayer(playerUUID);
        if(player != null) {
            player.sendRichMessage(msg);
        }
    }

    @Override
    public void sendMsg(Component component) {
        Player player = Bukkit.getPlayer(playerUUID);
        if(player != null) {
            player.sendMessage(component);
        }
    }

    @NotNull @Override
    public OfflinePlayer asOfflinePlayer() {
        return Bukkit.getOfflinePlayer(playerUUID);
    }

    @Override
    public FutureOperation<Optional<Punishment<?>>> fetchPunishment(PunishmentType punishmentType) {
        return CardinalProvider.provide().getPunishmentManager()
                .getActivePunishment(this.playerUUID, punishmentType);
    }
}
