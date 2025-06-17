package net.mineabyss.core.punishments.target;

import net.kyori.adventure.text.Component;
import net.mineabyss.cardinal.api.CardinalProvider;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.PunishableType;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.util.FutureOperation;
import net.mineabyss.core.util.IPUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class IPTarget implements Punishable<String> {

    private final @Nullable PlayerTarget target;
    private final UUID uuid;
    private final String ipAddress;
    private Instant lastSeen;

    IPTarget(@Nullable PlayerTarget target, String ipAddress) {
        this.target = target;
        this.uuid = IPUtils.ipToUUID(ipAddress);
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the unique identifier for this punishable entity.
     *
     * @return the unique identifier
     */
    @Override
    public @NotNull PunishableType getType() {
        return PunishableType.IP_ADDRESS;
    }

    /**
     * Returns the name of the target entity.
     *
     * @return the name of the target entity
     */
    @NotNull
    @Override
    public String getTargetName() {
        if (target == null) {
            return ipAddress; // Fallback to IP address if target is null
        }
        return target.getTargetName();
    }

    /**
     * Returns the UUID of the target entity.
     *
     * @return the UUID of the target entity
     */
    @Override
    public @NotNull UUID getTargetUUID() {
        return uuid;
    }

    /**
     * Returns the target entity itself.
     *
     * @return the target entity
     */
    @NotNull @Override
    public String getTarget() {
        return ipAddress;
    }

    /**
     * Returns the last seen time of the target entity.
     *
     * @return the last seen time as an Instant
     */
    @Override
    public Instant getLastSeen() {
        return lastSeen; // Return the last seen time of the IP if target is null
    }

    /**
     * Sets last seen time of the target entity to the current time.
     */
    @Override
    public void refreshLastSeen() {
        lastSeen = Instant.now(); // Update last seen for the IP if target is null
    }

    @Override
    public void kick(Component component) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress().equals(ipAddress)) {
                player.kick(component);
            }
        }
    }

    @Override
    public void sendMsg(String msg) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress().equals(ipAddress)) {
                player.sendRichMessage(msg);
            }
        }
    }

    @Override
    public void sendMsg(Component component) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress().equals(ipAddress)) {
                player.sendMessage(component);
            }
        }
    }

    @Override
    public @Nullable OfflinePlayer asOfflinePlayer() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress().equals(ipAddress)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public FutureOperation<Optional<Punishment<?>>> fetchPunishment(PunishmentType punishmentType) {
        return CardinalProvider.provide().getPunishmentManager()
                .getActiveIPPunishment(this.ipAddress, punishmentType);
    }

}
