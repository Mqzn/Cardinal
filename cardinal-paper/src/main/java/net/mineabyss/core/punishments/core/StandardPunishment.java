package net.mineabyss.core.punishments.core;

import com.mineabyss.lib.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentID;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.PunishmentRevision;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.core.storage.mongo.mapping.ExcludeField;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StandardPunishment<T> implements Punishment<T> {

    private final @NotNull PunishmentID id;

    @ExcludeField
    private final @NotNull PunishmentType type;

    private final @NotNull Punishable<T> target;

    private final @NotNull PunishmentIssuer issuer;

    private @Nullable String reason;

    private final Instant issuedAt;
    private Duration duration;
    private Instant expiresAt;

    private final List<String> notes = new ArrayList<>();
    private final List<PunishmentRevision> revisions = new ArrayList<>();

    private RevocationInfo revocationInfo;

    public StandardPunishment(
            @NotNull PunishmentID id,
            @NotNull PunishmentType type,
            @NotNull Punishable<T> target,
            @NotNull PunishmentIssuer issuer,
            @Nullable String reason,
            @NotNull Instant issuedAt,
            @NotNull Duration duration,
            @NotNull Instant expiresAt
    ) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.issuer = issuer;
        this.reason = reason;

        this.issuedAt = issuedAt;
        this.duration = duration;
        this.expiresAt = expiresAt;
        createInitialRevision();
    }

    public StandardPunishment(
            @NotNull PunishmentID id,
            @NotNull PunishmentType type,
            @NotNull Punishable<T> target,
            @NotNull PunishmentIssuer issuer,
            @Nullable String reason,
            @NotNull Instant issuedAt,
            @NotNull Duration duration
    ) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.issuer = issuer;
        this.reason = reason;

        this.issuedAt = issuedAt;
        this.duration = duration;
        this.expiresAt = issuedAt.plus(duration);
        createInitialRevision();
    }

    public StandardPunishment(
            @NotNull PunishmentType type,
            @NotNull Punishable<T> target,
            @NotNull PunishmentIssuer issuer,
            @NotNull Duration duration,
            @Nullable String reason
    ) {
        this(new StandardPunishmentID(), type, target, issuer, reason, Instant.now(), duration);
    }

    /**
     * Returns the unique identifier for this punishment.
     *
     * @return the unique identifier
     */
    @Override
    public @NotNull PunishmentID getId() {
        return id;
    }

    /**
     * Returns the type of this punishment.
     *
     * @return the type of the punishment
     */
    @Override
    public @NotNull PunishmentType getType() {
        return type;
    }

    /**
     * Returns the target of this punishment.
     * <p>
     * The target is an entity that can be punished, such as a player or an IP address.
     * </p>
     *
     * @return the target entity that is being punished
     */
    @Override
    public @NotNull Punishable<T> getTarget() {
        return target;
    }

    /**
     * Returns the {@link PunishmentIssuer} who issued this punishment.
     * <p>
     * The issuer is the entity that applied the punishment, such as a player or the console.
     * </p>
     *
     * @return the issuer of the punishment
     */
    @Override
    public @NotNull PunishmentIssuer getIssuer() {
        return issuer;
    }

    /**
     * Returns the reason for this punishment.
     * <p>
     * The reason is an optional string that explains why the punishment was applied.
     * </p>
     *
     * @return an Optional containing the reason, or empty if no reason was provided
     */
    @Override
    public @NotNull Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Returns the time when this punishment was issued.
     * <p>
     * This is represented as {@link Instant}, which provides a precise point in time.
     * </p>
     *
     * @return the time when the punishment was issued
     */
    @Override
    public @NotNull Instant getIssuedAt() {
        return issuedAt;
    }

    /**
     * Returns the duration of this punishment.
     * <p>
     * The duration is represented as a {@link Duration}, which indicates how long the punishment lasts.
     * </p>
     *
     * @return the duration of the punishment
     */
    @Override
    public @NotNull Duration getDuration() {
        return duration;
    }

    /**
     * Returns the time when this punishment expires.
     * <p>
     * This is represented as {@link Instant}, which provides a precise point in time.
     * </p>
     *
     * @return the expiration time of the punishment
     */
    @NotNull @Override
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Retrieves all notes associated with the punished player or entity.
     *
     * @return a list of strings representing the punishment-related notes.
     */
    @NotNull
    @Override
    public List<String> getNotes() {
        return notes;
    }

    @Override
    public void addNote(@NotNull String note) {
        Validate.notBlank(note, "note cannot be blank");
        this.notes.add(note);

        addRevision(PunishmentRevision.builder(this.id, PunishmentRevision.RevisionType.NOTE_ADDED)
                .issuer(this.issuer)
                .newValue(note)
                .reason("Note added to punishment record")
                .build());
    }

    @Override
    public void clearNotes() {
        if (!notes.isEmpty()) {
            int count = notes.size();
            notes.clear();

            addRevision(PunishmentRevision.builder(this.id, PunishmentRevision.RevisionType.NOTES_CLEARED)
                    .issuer(this.issuer)
                    .reason("Cleared " + count + " notes from punishment record")
                    .build());
        }
    }

    @Override
    public void setNotesTo(List<String> notes) {
        this.notes.clear();
        this.notes.addAll(notes);
    }

    /**
     * Checks if this punishment has been revoked.
     *
     * @return true if revoked, false otherwise
     */
    @Override
    public boolean isRevoked() {
        return revocationInfo != null;
    }

    /**
     * Gets the revocation information if this punishment was revoked.
     *
     * @return the revocation info, or empty if not revoked
     */
    @NotNull
    @Override
    public Optional<RevocationInfo> getRevocationInfo() {
        return Optional.ofNullable(revocationInfo);
    }

    /**
     * Revokes this punishment, and sets its {@link RevocationInfo}
     *
     * @param revocationInfo the info to set regarding the revocation.
     */
    @Override
    public void revoke(@NotNull RevocationInfo revocationInfo) {
        Validate.notNull(revocationInfo, "revocationInfo cannot be null");
        this.revocationInfo = revocationInfo;

        addRevision(PunishmentRevision.builder(this.id, PunishmentRevision.RevisionType.REVOKED)
                .issuer(revocationInfo.getRevoker())
                .reason(revocationInfo.getReason())
                .build());
    }

    /**
     * Sets the reason of a punishment
     *
     * @param newReason the new reason to set for the punishment
     */
    @Override
    public void setReason(@Nullable String newReason) {
        PunishmentRevision.Builder builder = PunishmentRevision.builder(this.id, PunishmentRevision.RevisionType.REASON_UPDATED)
                .issuer(this.issuer)
                .oldValue(this.reason)
                .newValue(newReason);

        if (this.reason != null && newReason != null) {
            builder.reason("Reason updated from '" + this.reason + "' to '" + newReason + "'");
        } else if (newReason != null) {
            builder.reason("Reason set to: " + newReason);
        } else {
            builder.reason("Reason cleared");
        }

        this.reason = newReason;
        addRevision(builder.build());
    }

    /**
     * Retrieves the immutable list of revision history for this punishment.
     * Each revision represents a modification to the punishment.
     *
     * @return an immutable list of punishment revisions
     */
    @NotNull
    public List<PunishmentRevision> getRevisions() {
        return Collections.unmodifiableList(revisions);
    }

    /**
     * Adds a new revision to this punishment's history.
     * This should be called whenever any significant change is made to the punishment.
     *
     * @param revision the revision to add
     */
    @Override
    public void addRevision(@NotNull PunishmentRevision revision) {
        Validate.notNull(revision, "revision cannot be null");
        revisions.add(revision);
    }

    /**
     * @return Whether the punishment is permanent or not.
     */
    @Override
    public boolean isPermanent() {
        return duration.isNegative() || duration.isZero();
    }

    @Override
    public void setRevokeInfo(@Nullable RevocationInfo revocationInfo) {
        this.revocationInfo = revocationInfo;
    }

    @Override
    public void setDuration(Duration duration) {
        this.duration = duration;
        this.expiresAt = issuedAt.plus(this.duration);
    }

    @NotNull
    @Override
    public TagResolver asTagResolver() {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return TagResolver.builder()
                .tag("punishment_target", Tag.preProcessParsed(target.getTargetName()))
                .tag("punishment_issuer", Tag.preProcessParsed(issuer.getName()))
                .tag("punishment_reason", Tag.preProcessParsed(getReason().orElse("N/A")))
                .tag("punishment_id", Tag.preProcessParsed(id.getRepresentation()))
                .tag("punishment_issued_date", Tag.preProcessParsed(TimeUtil.formatDate(this.issuedAt)))
                .tag("punishment_expires_date", Tag.preProcessParsed(TimeUtil.formatDate(this.expiresAt)))
                .tag("punishment_duration", Tag.preProcessParsed( this.isPermanent() ? "∞" : TimeUtil.format(this.duration)) )
                .tag("punishment_time_left", Tag.preProcessParsed(this.isPermanent() ? "∞" : TimeUtil.format(remaining)))
                .build();
    }

    /**
     * Creates the initial revision when the punishment is first created.
     */
    private void createInitialRevision() {
        String reasonMsg = this.reason != null ?
                "Punishment created with reason: " + this.reason :
                "Punishment created without reason";

        addRevision(PunishmentRevision.builder(this.id, PunishmentRevision.RevisionType.CREATED)
                .issuer(this.issuer)
                .reason(reasonMsg)
                .newDuration(this.duration)
                .build());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StandardPunishment<?> that)) {
            return false;
        }
        return Objects.equals(id.getRepresentation(), that.id.getRepresentation());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id.getRepresentation());
    }


    public static final class StandardRevocationInfo implements Punishment.RevocationInfo {

        private final PunishmentIssuer revoker;
        private Instant revokedAt = Instant.now();
        private final String reason;

        public StandardRevocationInfo(PunishmentIssuer revoker, String reason) {
            this.revoker = revoker;
            this.reason = reason;
        }

        public StandardRevocationInfo(PunishmentIssuer revoker, Instant revokedAt, String reason) {
            this.revoker = revoker;
            this.revokedAt = revokedAt;
            this.reason = reason;
        }


        /**
         * Gets the issuer who revoked the punishment.
         *
         * @return the revoker
         */
        @NotNull
        @Override
        public PunishmentIssuer getRevoker() {
            return revoker;
        }

        /**
         * Gets the timestamp when the punishment was revoked.
         *
         * @return the revocation timestamp
         */
        @NotNull
        @Override
        public Instant getRevokedAt() {
            return revokedAt;
        }

        /**
         * Gets the reason for revocation.
         *
         * @return the revocation reason
         */
        @NotNull
        @Override
        public String getReason() {
            return reason;
        }
    }
}
