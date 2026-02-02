package eg.mqzen.cardinal.api.punishments;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Represents a revision/change made to a punishment record.
 * <p>
 * This immutable class tracks modifications to punishments, including reason updates,
 * duration changes, revocations, and other administrative actions. It provides
 * an audit trail for punishment management.
 * </p>
 *
 * @since 1.0
 * @see PunishmentHistoryService#getPunishmentRevisions(PunishmentID)
 */
public final class PunishmentRevision {

    /**
     * Enumeration of possible revision types.
     */
    public enum RevisionType {

        /** The punishment was created */
        CREATED,

        /** The punishment reason was updated */
        REASON_UPDATED,

        /** The punishment duration was modified */
        DURATION_MODIFIED,

        /** The punishment was revoked/cancelled */
        REVOKED,

        /** The punishment was appealed */
        APPEALED,

        /** The punishment appeal was approved */
        APPEAL_APPROVED,

        /** The punishment appeal was denied */
        APPEAL_DENIED,

        /** The punishment was automatically expired */
        EXPIRED,

        /** The punishment was restored after being revoked */
        RESTORED,

        /** Administrative note was added */
        NOTE_ADDED,

        /**All administrative notes were removed.*/
        NOTES_CLEARED,

        /** Punishment was transferred to different issuer */
        TRANSFERRED
    }

    private final @NotNull PunishmentID punishmentId;
    private final @NotNull RevisionType revisionType;
    private final @NotNull Instant timestamp;
    private final @Nullable PunishmentIssuer issuer;
    private final @Nullable String oldValue;
    private final @Nullable String newValue;
    private final @Nullable String reason;
    private final @NotNull Map<String, String> metadata;
    private final @Nullable Duration oldDuration;
    private final @Nullable Duration newDuration;

    private PunishmentRevision(@NotNull Builder builder) {
        this.punishmentId = Objects.requireNonNull(builder.punishmentId, "punishmentId cannot be null");
        this.revisionType = Objects.requireNonNull(builder.revisionType, "revisionType cannot be null");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.issuer = builder.issuer;
        this.oldValue = builder.oldValue;
        this.newValue = builder.newValue;
        this.reason = builder.reason;
        this.metadata = Map.copyOf(builder.metadata);
        this.oldDuration = builder.oldDuration;
        this.newDuration = builder.newDuration;
    }

    // Getters
    public @NotNull PunishmentID getPunishmentId() { return punishmentId; }
    public @NotNull RevisionType getRevisionType() { return revisionType; }
    public @NotNull Instant getTimestamp() { return timestamp; }
    public @NotNull Optional<PunishmentIssuer> getIssuer() { return Optional.ofNullable(issuer); }
    public @NotNull Optional<String> getOldValue() { return Optional.ofNullable(oldValue); }
    public @NotNull Optional<String> getNewValue() { return Optional.ofNullable(newValue); }
    public @NotNull Optional<String> getReason() { return Optional.ofNullable(reason); }
    public @NotNull Map<String, String> getMetadata() { return metadata; }
    public @NotNull Optional<Duration> getOldDuration() { return Optional.ofNullable(oldDuration); }
    public @NotNull Optional<Duration> getNewDuration() { return Optional.ofNullable(newDuration); }

    /**
     * Gets a specific metadata value.
     *
     * @param key the metadata key
     * @return the metadata value, or null if not present
     */
    public @Nullable String getMetadata(@NotNull String key) {
        Validate.notNull(key, "key cannot be null");
        return metadata.get(key);
    }

    /**
     * Checks if this revision represents a structural change to the punishment.
     *
     * @return true if this is a structural change (creation, revocation, duration change)
     */
    public boolean isStructuralChange() {
        return revisionType == RevisionType.CREATED ||
               revisionType == RevisionType.REVOKED ||
               revisionType == RevisionType.DURATION_MODIFIED ||
               revisionType == RevisionType.EXPIRED ||
               revisionType == RevisionType.RESTORED;
    }

    /**
     * Checks if this revision represents an administrative action.
     *
     * @return true if this is an administrative action
     */
    public boolean isAdministrativeAction() {
        return revisionType == RevisionType.APPEALED ||
               revisionType == RevisionType.APPEAL_APPROVED ||
               revisionType == RevisionType.APPEAL_DENIED ||
               revisionType == RevisionType.NOTE_ADDED ||
               revisionType == RevisionType.TRANSFERRED;
    }

    /**
     * Creates a new builder instance.
     *
     * @param punishmentId the ID of the punishment this revision is for
     * @param revisionType the type of revision
     * @return a new builder for creating punishment revisions
     */
    public static @NotNull Builder builder(@NotNull PunishmentID punishmentId, @NotNull RevisionType revisionType) {
        return new Builder(punishmentId, revisionType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PunishmentRevision that = (PunishmentRevision) obj;
        return Objects.equals(punishmentId, that.punishmentId) &&
               revisionType == that.revisionType &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(issuer, that.issuer) &&
               Objects.equals(oldValue, that.oldValue) &&
               Objects.equals(newValue, that.newValue) &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(oldDuration, that.oldDuration) &&
               Objects.equals(newDuration, that.newDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(punishmentId, revisionType, timestamp, issuer, oldValue,
                          newValue, reason, metadata, oldDuration, newDuration);
    }

    @Override
    public String toString() {
        return "PunishmentRevision{" +
               "punishmentId=" + punishmentId +
               ", revisionType=" + revisionType +
               ", timestamp=" + timestamp +
               ", issuer=" + issuer +
               ", oldValue='" + oldValue + '\'' +
               ", newValue='" + newValue + '\'' +
               ", reason='" + reason + '\'' +
               ", oldDuration=" + oldDuration +
               ", newDuration=" + newDuration +
               ", metadata=" + metadata +
               '}';
    }

    /**
     * Builder class for constructing {@link PunishmentRevision} instances.
     */
    public static final class Builder {
        private final @NotNull PunishmentID punishmentId;
        private final @NotNull RevisionType revisionType;
        private @NotNull Instant timestamp = Instant.now();
        private @Nullable PunishmentIssuer issuer;
        private @Nullable String oldValue;
        private @Nullable String newValue;
        private @Nullable String reason;
        private final @NotNull Map<String, String> metadata = new HashMap<>();
        private @Nullable Duration oldDuration;
        private @Nullable Duration newDuration;

        private Builder(@NotNull PunishmentID punishmentId, @NotNull RevisionType revisionType) {
            this.punishmentId = Objects.requireNonNull(punishmentId, "punishmentId cannot be null");
            this.revisionType = Objects.requireNonNull(revisionType, "revisionType cannot be null");
        }

        public @NotNull Builder timestamp(@NotNull Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
            return this;
        }

        public @NotNull Builder issuer(@Nullable PunishmentIssuer issuer) {
            this.issuer = issuer;
            return this;
        }

        public @NotNull Builder oldValue(@Nullable String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public @NotNull Builder newValue(@Nullable String newValue) {
            this.newValue = newValue;
            return this;
        }

        public @NotNull Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        public @NotNull Builder addMetadata(@NotNull String key, @NotNull String value) {
            Validate.notNull(key, "key cannot be null");
            Validate.notNull(value, "value cannot be null");
            this.metadata.put(key, value);
            return this;
        }

        public @NotNull Builder metadata(@NotNull Map<String, String> metadata) {
            Validate.notNull(metadata, "metadata cannot be null");
            Validate.noNullElements(metadata.keySet(), "metadata keys cannot be null");
            Validate.noNullElements(metadata.values(), "metadata values cannot be null");
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return this;
        }

        public @NotNull Builder oldDuration(@Nullable Duration oldDuration) {
            this.oldDuration = oldDuration;
            return this;
        }

        public @NotNull Builder newDuration(@Nullable Duration newDuration) {
            this.newDuration = newDuration;
            return this;
        }

        public @NotNull Builder durationChange(@Nullable Duration oldDuration, @Nullable Duration newDuration) {
            this.oldDuration = oldDuration;
            this.newDuration = newDuration;
            return this;
        }

        /**
         * Builds the revision with validation.
         *
         * @return an immutable {@link PunishmentRevision} instance
         * @throws IllegalArgumentException if any validation constraints are violated
         */
        public @NotNull PunishmentRevision build() {
            validateConstraints();
            return new PunishmentRevision(this);
        }

        private void validateConstraints() {
            // Validate that duration changes have both old and new values when type is DURATION_MODIFIED
            if (revisionType == RevisionType.DURATION_MODIFIED) {
                if (oldDuration == null && newDuration == null) {
                    throw new IllegalArgumentException("Duration modification must have at least one duration value");
                }
            }

            // Validate that reason updates have both old and new values when type is REASON_UPDATED
            if (revisionType == RevisionType.REASON_UPDATED) {
                if (oldValue == null && newValue == null) {
                    throw new IllegalArgumentException("Reason update must have at least one reason value");
                }
            }
        }
    }
}