package net.mineabyss.cardinal.api.punishments;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Immutable criteria builder for advanced punishment searching.
 * <p>
 * This class follows the builder pattern to construct search criteria for punishment queries.
 * All instances are immutable once built, ensuring thread safety and preventing accidental modifications.
 * </p>
 *
 * @since 1.0
 * @see PunishmentManager#searchPunishments(PunishmentSearchCriteria, int)
 */
public final class PunishmentSearchCriteria {

    private final @Nullable PunishmentType type;
    private final @Nullable PunishmentIssuer issuer;
    private final @Nullable UUID targetPlayerId;
    private final @Nullable String reason;
    private final @Nullable Instant issuedAfter;
    private final @Nullable Instant issuedBefore;
    private final @Nullable Instant expiresAfter;
    private final @Nullable Instant expiresBefore;
    private final @Nullable Duration minDuration;
    private final @Nullable Duration maxDuration;
    private final @Nullable Boolean activeOnly;
    private final @Nullable Boolean permanentOnly;
    private final @NotNull Set<PunishmentType> excludeTypes;

    /**
     * Private constructor used by the builder.
     */
    private PunishmentSearchCriteria(@NotNull Builder builder) {
        this.type = builder.type;
        this.issuer = builder.issuer;
        this.targetPlayerId = builder.targetPlayerId;
        this.reason = builder.reason;
        this.issuedAfter = builder.issuedAfter;
        this.issuedBefore = builder.issuedBefore;
        this.expiresAfter = builder.expiresAfter;
        this.expiresBefore = builder.expiresBefore;
        this.minDuration = builder.minDuration;
        this.maxDuration = builder.maxDuration;
        this.activeOnly = builder.activeOnly;
        this.permanentOnly = builder.permanentOnly;
        this.excludeTypes = Set.copyOf(builder.excludeTypes);
    }

    // Getters
    public @Nullable PunishmentType getType() { return type; }
    public @Nullable PunishmentIssuer getIssuer() { return issuer; }
    public @Nullable UUID getTargetPlayerId() { return targetPlayerId; }
    public @Nullable String getReason() { return reason; }
    public @Nullable Instant getIssuedAfter() { return issuedAfter; }
    public @Nullable Instant getIssuedBefore() { return issuedBefore; }
    public @Nullable Instant getExpiresAfter() { return expiresAfter; }
    public @Nullable Instant getExpiresBefore() { return expiresBefore; }
    public @Nullable Duration getMinDuration() { return minDuration; }
    public @Nullable Duration getMaxDuration() { return maxDuration; }
    public @Nullable Boolean getActiveOnly() { return activeOnly; }
    public @Nullable Boolean getPermanentOnly() { return permanentOnly; }
    public @NotNull Set<PunishmentType> getExcludeTypes() { return excludeTypes; }

    /**
     * Creates a new builder instance.
     *
     * @return a new builder for creating search criteria
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder instance pre-populated with values from this criteria.
     *
     * @return a new builder with this criteria's values
     */
    public @NotNull Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Checks if any search criteria have been specified.
     *
     * @return true if this criteria has any non-null values, false if it's empty
     */
    public boolean isEmpty() {
        return type == null && issuer == null && targetPlayerId == null &&
               reason == null && issuedAfter == null && issuedBefore == null &&
               expiresAfter == null && expiresBefore == null && minDuration == null &&
               maxDuration == null && activeOnly == null && permanentOnly == null &&
               excludeTypes.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PunishmentSearchCriteria that = (PunishmentSearchCriteria) obj;
        return Objects.equals(type, that.type) &&
               Objects.equals(issuer, that.issuer) &&
               Objects.equals(targetPlayerId, that.targetPlayerId) &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(issuedAfter, that.issuedAfter) &&
               Objects.equals(issuedBefore, that.issuedBefore) &&
               Objects.equals(expiresAfter, that.expiresAfter) &&
               Objects.equals(expiresBefore, that.expiresBefore) &&
               Objects.equals(minDuration, that.minDuration) &&
               Objects.equals(maxDuration, that.maxDuration) &&
               Objects.equals(activeOnly, that.activeOnly) &&
               Objects.equals(permanentOnly, that.permanentOnly) &&
               Objects.equals(excludeTypes, that.excludeTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, issuer, targetPlayerId, reason, issuedAfter,
                          issuedBefore, expiresAfter, expiresBefore, minDuration, maxDuration,
                          activeOnly, permanentOnly, excludeTypes);
    }

    @Override
    public String toString() {
        return "PunishmentSearchCriteria{" +
               "type=" + type +
               ", issuer=" + issuer +
               ", targetPlayerId=" + targetPlayerId +
               ", reasonContains='" + reason + '\'' +
               ", issuedAfter=" + issuedAfter +
               ", issuedBefore=" + issuedBefore +
               ", expiresAfter=" + expiresAfter +
               ", expiresBefore=" + expiresBefore +
               ", minDuration=" + minDuration +
               ", maxDuration=" + maxDuration +
               ", activeOnly=" + activeOnly +
               ", permanentOnly=" + permanentOnly +
               ", excludeTypes=" + excludeTypes +
               '}';
    }

    /**
     * Builder class for constructing {@link PunishmentSearchCriteria} instances.
     * <p>
     * This builder follows the fluent interface pattern and validates constraints
     * when the criteria is built.
     * </p>
     */
    public static final class Builder {
        private @Nullable PunishmentType type;
        private @Nullable PunishmentIssuer issuer;
        private @Nullable UUID targetPlayerId;
        private @Nullable String reason;
        private @Nullable Instant issuedAfter;
        private @Nullable Instant issuedBefore;
        private @Nullable Instant expiresAfter;
        private @Nullable Instant expiresBefore;
        private @Nullable Duration minDuration;
        private @Nullable Duration maxDuration;
        private @Nullable Boolean activeOnly;
        private @Nullable Boolean permanentOnly;
        private final @NotNull Set<PunishmentType> excludeTypes = new HashSet<>();

        private Builder() {}

        private Builder(@NotNull PunishmentSearchCriteria criteria) {
            this.type = criteria.type;
            this.issuer = criteria.issuer;
            this.targetPlayerId = criteria.targetPlayerId;
            this.reason = criteria.reason;
            this.issuedAfter = criteria.issuedAfter;
            this.issuedBefore = criteria.issuedBefore;
            this.expiresAfter = criteria.expiresAfter;
            this.expiresBefore = criteria.expiresBefore;
            this.minDuration = criteria.minDuration;
            this.maxDuration = criteria.maxDuration;
            this.activeOnly = criteria.activeOnly;
            this.permanentOnly = criteria.permanentOnly;
            this.excludeTypes.addAll(criteria.excludeTypes);
        }

        public @NotNull Builder type(@Nullable PunishmentType type) {
            this.type = type;
            return this;
        }

        public @NotNull Builder issuer(@Nullable PunishmentIssuer issuer) {
            this.issuer = issuer;
            return this;
        }

        public @NotNull Builder targetPlayerId(@Nullable UUID targetPlayerId) {
            this.targetPlayerId = targetPlayerId;
            return this;
        }

        public @NotNull Builder reasonContains(@Nullable String reasonContains) {
            this.reason = reasonContains != null && reasonContains.trim().isEmpty() ? null : reasonContains;
            return this;
        }

        public @NotNull Builder issuedAfter(@Nullable Instant issuedAfter) {
            this.issuedAfter = issuedAfter;
            return this;
        }

        public @NotNull Builder issuedBefore(@Nullable Instant issuedBefore) {
            this.issuedBefore = issuedBefore;
            return this;
        }

        public @NotNull Builder issuedBetween(@NotNull Instant after, @NotNull Instant before) {
            Validate.notNull(after, "issuedAfter cannot be null");
            Validate.notNull(before, "issuedBefore cannot be null");
            Validate.isTrue(after.isBefore(before), "issuedAfter must be before issuedBefore");
            
            this.issuedAfter = after;
            this.issuedBefore = before;
            return this;
        }

        public @NotNull Builder expiresAfter(@Nullable Instant expiresAfter) {
            this.expiresAfter = expiresAfter;
            return this;
        }

        public @NotNull Builder expiresBefore(@Nullable Instant expiresBefore) {
            this.expiresBefore = expiresBefore;
            return this;
        }

        public @NotNull Builder expiresBetween(@NotNull Instant after, @NotNull Instant before) {
            Validate.notNull(after, "expiresAfter cannot be null");
            Validate.notNull(before, "expiresBefore cannot be null");
            Validate.isTrue(after.isBefore(before), "expiresAfter must be before expiresBefore");
            
            this.expiresAfter = after;
            this.expiresBefore = before;
            return this;
        }

        public @NotNull Builder minDuration(@Nullable Duration minDuration) {
            if (minDuration != null) {
                Validate.isTrue(!minDuration.isNegative(), "minDuration cannot be negative");
            }
            this.minDuration = minDuration;
            return this;
        }

        public @NotNull Builder maxDuration(@Nullable Duration maxDuration) {
            if (maxDuration != null) {
                Validate.isTrue(!maxDuration.isNegative(), "maxDuration cannot be negative");
            }
            this.maxDuration = maxDuration;
            return this;
        }

        public @NotNull Builder durationBetween(@NotNull Duration min, @NotNull Duration max) {
            Validate.notNull(min, "minDuration cannot be null");
            Validate.notNull(max, "maxDuration cannot be null");
            Validate.isTrue(!min.isNegative(), "minDuration cannot be negative");
            Validate.isTrue(!max.isNegative(), "maxDuration cannot be negative");
            Validate.isTrue(min.compareTo(max) <= 0, "minDuration must be less than or equal to maxDuration");
            
            this.minDuration = min;
            this.maxDuration = max;
            return this;
        }

        public @NotNull Builder activeOnly(boolean activeOnly) {
            this.activeOnly = activeOnly;
            return this;
        }

        public @NotNull Builder permanentOnly(boolean permanentOnly) {
            this.permanentOnly = permanentOnly;
            return this;
        }

        public @NotNull Builder excludeType(@NotNull PunishmentType type) {
            Validate.notNull(type, "type cannot be null");
            this.excludeTypes.add(type);
            return this;
        }

        public @NotNull Builder excludeTypes(@NotNull Collection<PunishmentType> types) {
            Validate.notNull(types, "types cannot be null");
            Validate.noNullElements(types, "types cannot contain null elements");
            this.excludeTypes.addAll(types);
            return this;
        }

        public @NotNull Builder clearExcludeTypes() {
            this.excludeTypes.clear();
            return this;
        }

        /**
         * Builds the search criteria with validation.
         *
         * @return an immutable {@link PunishmentSearchCriteria} instance
         * @throws IllegalArgumentException if any validation constraints are violated
         */
        public @NotNull PunishmentSearchCriteria build() {
            validateConstraints();
            return new PunishmentSearchCriteria(this);
        }

        private void validateConstraints() {
            if (issuedAfter != null && issuedBefore != null) {
                Validate.isTrue(issuedAfter.isBefore(issuedBefore), 
                    "issuedAfter must be before issuedBefore");
            }
            
            if (expiresAfter != null && expiresBefore != null) {
                Validate.isTrue(expiresAfter.isBefore(expiresBefore), 
                    "expiresAfter must be before expiresBefore");
            }
            
            if (minDuration != null && maxDuration != null) {
                Validate.isTrue(minDuration.compareTo(maxDuration) <= 0, 
                    "minDuration must be less than or equal to maxDuration");
            }
            
            if (type != null && excludeTypes.contains(type)) {
                throw new IllegalArgumentException("Cannot both include and exclude the same punishment type: " + type);
            }
        }
    }
}
