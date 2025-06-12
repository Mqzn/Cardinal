package net.mineabyss.core.punishments;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import org.jetbrains.annotations.Nullable;
import java.util.Deque;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class PunishmentsCache {
    @Getter
    private final PunishmentType type;
    private final Cache<UUID, Deque<Punishment<?>>> internal;

    public PunishmentsCache(PunishmentType type) {
        this.type = type;
        this.internal = Caffeine.newBuilder()
                .maximumSize(1000) // Prevent unbounded growth
                .build();
    }

    /**
     * Retrieves punishments for a player, returning an empty deque if none exist.
     * This prevents null pointer exceptions and provides a consistent API.
     */
    public @NotNull Deque<Punishment<?>> getPunishments(@NotNull UUID uuid) {
        return Objects.requireNonNull(internal.get(uuid, k -> new ConcurrentLinkedDeque<>()));
    }

    /**
     * Gets punishments if they exist in cache, or null if not cached.
     * Useful when you want to distinguish between "no punishments" and "not cached".
     */
    public @Nullable Deque<Punishment<?>> getPunishmentsIfPresent(@NotNull UUID uuid) {
        return internal.getIfPresent(uuid);
    }

    /**
     * Adds a punishment to the player's record.
     * Creates a new deque if this is the player's first punishment.
     */
    public void addPunishment(@NotNull UUID uuid, @NotNull Punishment<?> punishment) {

        internal.asMap().compute(uuid, (oldId, otherActivePunishments)-> {
            if(otherActivePunishments == null) {
               Deque<Punishment<?>> newStart = new ArrayDeque<>();
               newStart.add(punishment);
               return newStart;
           }

           otherActivePunishments.add(punishment);
           return otherActivePunishments;
        });
    }

    /**
     * Adds multiple punishments atomically.
     */
    public void addPunishments(@NotNull UUID uuid, @NotNull Collection<Punishment<?>> punishments) {
        if (punishments.isEmpty()) return;

        Deque<Punishment<?>> playerPunishments = internal.get(uuid, k -> new ConcurrentLinkedDeque<>());
        assert playerPunishments != null;
        playerPunishments.addAll(punishments);
    }


    public void setPunishmentsFor(UUID punishmentsOwner, Deque<Punishment<?>> listOfPunishments) {
        internal.put(punishmentsOwner, listOfPunishments);
    }

    /**
     * Removes a specific punishment from the player's record.
     * Returns true if the punishment was found and removed.
     */
    public void removePunishment(@NotNull UUID uuid, @NotNull Punishment<?> punishment) {
        internal.asMap().compute(uuid,(oldId, otherActivePunishments)-> {
            if(otherActivePunishments == null) {
                Deque<Punishment<?>> newPunishmentsStart = new ArrayDeque<>();
                newPunishmentsStart.add(punishment);
                return newPunishmentsStart;
            }

            otherActivePunishments.remove(punishment);
            return otherActivePunishments;
        });
    }

    /**
     * Clears all punishments for a player.
     */
    public void clearPunishments(@NotNull UUID uuid) {
        internal.invalidate(uuid);
    }

    /**
     * Gets the most recent punishment for a player, or null if none exist.
     */
    public @Nullable Punishment<?> getLatestPunishment(@NotNull UUID uuid) {
        Deque<Punishment<?>> punishments = internal.getIfPresent(uuid);
        return punishments != null ? punishments.peekLast() : null;
    }

    /**
     * Gets the number of punishments for a player.
     */
    public int getPunishmentCount(@NotNull UUID uuid) {
        Deque<Punishment<?>> punishments = internal.getIfPresent(uuid);
        return punishments != null ? punishments.size() : 0;
    }

    /**
     * Checks if a player has any punishments.
     */
    public boolean hasPunishments(@NotNull UUID uuid) {
        Deque<Punishment<?>> punishments = internal.getIfPresent(uuid);
        return punishments != null && !punishments.isEmpty();
    }

    /**
     * Gets cache statistics for monitoring and debugging.
     */
    public String getCacheStats() {
        return internal.stats().toString();
    }

    /**
     * Manually triggers cache cleanup.
     */
    public void cleanup() {
        internal.cleanUp();
    }

    /**
     * Gets the current cache size.
     */
    public long size() {
        return internal.estimatedSize();
    }

}
