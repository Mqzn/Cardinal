package net.mineabyss.cardinal.api.punishments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable statistical information about a player's punishment history.
 */
public final class PunishmentStatistics {
    private final @NotNull UUID playerId;
    private final int totalCount;
    private final int activeCount;
    private final int expiredCount;
    private final int revokedCount;
    private final int permanentCount;
    private final @NotNull Map<PunishmentType, Integer> countByType;
    private final @NotNull Map<PunishmentIssuer, Integer> countByIssuer;
    private final @Nullable Instant firstPunishment;
    private final @Nullable Instant lastPunishment;
    private final @Nullable Instant mostRecentActive;
    private final @NotNull Duration totalDuration;
    private final @NotNull Duration averageDuration;
    private final double punishmentsPerDay;
    private final @NotNull Instant generatedAt;

    private PunishmentStatistics(@NotNull Builder builder) {
        this.playerId = builder.playerId;
        this.totalCount = builder.totalCount;
        this.activeCount = builder.activeCount;
        this.expiredCount = builder.expiredCount;
        this.revokedCount = builder.revokedCount;
        this.permanentCount = builder.permanentCount;
        this.countByType = Map.copyOf(builder.countByType);
        this.countByIssuer = Map.copyOf(builder.countByIssuer);
        this.firstPunishment = builder.firstPunishment;
        this.lastPunishment = builder.lastPunishment;
        this.mostRecentActive = builder.mostRecentActive;
        this.totalDuration = builder.totalDuration;
        this.averageDuration = builder.averageDuration;
        this.punishmentsPerDay = builder.punishmentsPerDay;
        this.generatedAt = builder.generatedAt;
    }

    // Getters
    public @NotNull UUID getPlayerId() { return playerId; }
    public int getTotalCount() { return totalCount; }
    public int getActiveCount() { return activeCount; }
    public int getExpiredCount() { return expiredCount; }
    public int getRevokedCount() { return revokedCount; }
    public int getPermanentCount() { return permanentCount; }
    public @NotNull Map<PunishmentType, Integer> getCountByType() { return countByType; }
    public @NotNull Map<PunishmentIssuer, Integer> getCountByIssuer() { return countByIssuer; }
    public @NotNull Optional<Instant> getFirstPunishment() { return Optional.ofNullable(firstPunishment); }
    public @NotNull Optional<Instant> getLastPunishment() { return Optional.ofNullable(lastPunishment); }
    public @NotNull Optional<Instant> getMostRecentActive() { return Optional.ofNullable(mostRecentActive); }
    public @NotNull Duration getTotalDuration() { return totalDuration; }
    public @NotNull Duration getAverageDuration() { return averageDuration; }
    public double getPunishmentsPerDay() { return punishmentsPerDay; }
    public @NotNull Instant getGeneratedAt() { return generatedAt; }

    /**
     * Gets count for specific punishment type.
     */
    public int getCount(PunishmentType type) {
        return countByType.getOrDefault(type, 0);
    }

    /**
     * Gets count for specific issuer.
     */
    public int getCount(PunishmentIssuer issuer) {
        return countByIssuer.getOrDefault(issuer, 0);
    }

    /**
     * Checks if player has any active punishments.
     */
    public boolean hasActivePunishments() {
        return activeCount > 0;
    }

    /**
     * Gets most common punishment type.
     */
    public Optional<PunishmentType> getMostCommonType() {
        return countByType.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    /**
     * Gets most active issuer.
     */
    public Optional<PunishmentIssuer> getMostActiveIssuer() {
        return countByIssuer.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    /**
     * Creates a new builder instance.
     */
    public static Builder builder(UUID playerId) {
        return new Builder(playerId);
    }

    /**
     * Builder for PunishmentStatistics.
     */
    public static final class Builder {
        private final UUID playerId;
        private int totalCount = 0;
        private int activeCount = 0;
        private int expiredCount = 0;
        private int revokedCount = 0;
        private int permanentCount = 0;
        private final Map<PunishmentType, Integer> countByType = new ConcurrentHashMap<>();
        public final Map<PunishmentIssuer, Integer> countByIssuer = new ConcurrentHashMap<>();
        private Instant firstPunishment;
        private Instant lastPunishment;
        private Instant mostRecentActive;
        private Duration totalDuration = Duration.ZERO;
        private Duration averageDuration = Duration.ZERO;
        private double punishmentsPerDay = 0.0;
        private final Instant generatedAt = Instant.now();

        private Builder(UUID playerId) {
            this.playerId = Objects.requireNonNull(playerId);
        }

        /**
         * Adds a punishment to the statistics.
         */
        public synchronized void addPunishment(PunishmentType type, Duration duration) {
            totalCount++;
            
            // Update type count
            countByType.merge(type, 1, Integer::sum);
            
            // Update duration statistics
            if (duration != null) {
                if (duration.isNegative() || duration.isZero()) {
                    permanentCount++;
                } else {
                    totalDuration = totalDuration.plus(duration);
                }
            }
        }

        /**
         * Sets active punishments count.
         */
        public synchronized Builder activeCount(int count) {
            this.activeCount = count;
            return this;
        }

        /**
         * Sets expired punishments count.
         */
        public synchronized Builder expiredCount(int count) {
            this.expiredCount = count;
            return this;
        }

        /**
         * Sets revoked punishments count.
         */
        public synchronized Builder revokedCount(int count) {
            this.revokedCount = count;
            return this;
        }

        /**
         * Sets first punishment timestamp.
         */
        public synchronized Builder firstPunishment(Instant instant) {
            this.firstPunishment = instant;
            return this;
        }

        /**
         * Sets last punishment timestamp.
         */
        public synchronized Builder lastPunishment(Instant instant) {
            this.lastPunishment = instant;
            return this;
        }

        /**
         * Sets most recent active punishment timestamp.
         */
        public synchronized Builder mostRecentActive(Instant instant) {
            this.mostRecentActive = instant;
            return this;
        }

        /**
         * Finalizes and builds the statistics.
         */
        public synchronized PunishmentStatistics build() {
            // Calculate averages
            if (totalCount > 0) {
                this.averageDuration = totalDuration.dividedBy(totalCount - permanentCount);
                
                long days = Duration.between(firstPunishment, generatedAt).toDays();
                this.punishmentsPerDay = days > 0 ? (double) totalCount / days : totalCount;
            }
            
            return new PunishmentStatistics(this);
        }
    }
}