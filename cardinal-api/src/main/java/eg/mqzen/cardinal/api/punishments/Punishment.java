package eg.mqzen.cardinal.api.punishments;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import eg.mqzen.cardinal.api.storage.DBEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Represents a punishment applied to a target entity.
 * This interface provides methods to retrieve the punishment ID, type, target, issuer, and reason.
 *
 * @param <T> the type of the target entity (e.g., Player, IPAddress)
 */
public interface Punishment<T> extends DBEntity<String> {

    @NotNull @Override
    default String getEntityID() {
        return getId().getRepresentation();
    }

    /**
     * Returns the unique identifier for this punishment.
     *
     * @return the unique identifier
     */
    @NotNull PunishmentID getId();

    /**
     * Returns the type of this punishment.
     *
     * @return the type of the punishment
     */
    @NotNull PunishmentType getType();

    /**
     * Returns the target of this punishment.
     * <p>
     * The target is an entity that can be punished, such as a player or an IP address.
     * </p>
     *
     * @return the target entity that is being punished
     */
    @NotNull Punishable<T> getTarget();

    /**
     * Returns the {@link PunishmentIssuer} who issued this punishment.
     * <p>
     * The issuer is the entity that applied the punishment, such as a player or the console.
     * </p>
     *
     * @return the issuer of the punishment
     */
    @NotNull PunishmentIssuer getIssuer();

    /**
     * Returns the reason for this punishment.
     * <p>
     * The reason is an optional string that explains why the punishment was applied.
     * </p>
     *
     * @return an Optional containing the reason, or empty if no reason was provided
     */
    @NotNull Optional<String> getReason();

    /**
     * Returns the time when this punishment was issued.
     * <p>
     * This is represented as {@link Instant}, which provides a precise point in time.
     * </p>
     *
     * @return the time when the punishment was issued
     */
    @NotNull Instant getIssuedAt();

    /**
     * Returns the duration of this punishment.
     * <p>
     * The duration is represented as a {@link Duration}, which indicates how long the punishment lasts.
     * </p>
     *
     * @return the duration of the punishment
     */
    @NotNull Duration getDuration();

    /**
     * Returns the time when this punishment expires.
     * <p>
     * This is represented as {@link Instant}, which provides a precise point in time.
     * </p>
     *
     * @return the expiration time of the punishment
     */
    @Nullable Instant getExpiresAt();

    /**
     * Retrieves all notes associated with the punished player or entity.
     *
     * @return a list of strings representing the punishment-related notes.
     */
    @NotNull List<String> getNotes();

    /**
     * Adds a new note to the punished player or entity's record.
     * Notes are typically used to store contextual or historical information
     * about punishments or player behavior that may not warrant a formal punishment.
     *
     * @param note the note to add; should not be null or empty.
     */
    void addNote(@NotNull String note);

    /**
     * Clears all existing notes from the punished player or entity's record.
     * This action is irreversible and should be used with caution.
     */
    void clearNotes();

    /**
     * Sets all the notes to the given list of notes.
     * @param notes the notes to set.
     */
    void setNotesTo(List<String> notes);

    /**
     * Checks if this punishment has been revoked.
     *
     * @return true if revoked, false otherwise
     */
    boolean isRevoked();

    /**
     * Gets the revocation information if this punishment was revoked.
     *
     * @return the revocation info, or empty if not revoked
     */
    @NotNull Optional<RevocationInfo> getRevocationInfo();

    /**
     * Revokes this punishment, and sets its {@link RevocationInfo}
     * @param revocationInfo the info to set regarding the revocation.
     */
    void revoke(@NotNull RevocationInfo revocationInfo);

    /**
     * Sets the reason of a punishment
     * @param newReason the new reason to set for the punishment
     */
    void setReason(String newReason);

    /**
     * @return All revisions related to the {@link Punishment}
     */
    List<PunishmentRevision> getRevisions();

    /**
     * Adds a {@link PunishmentRevision} to the revisions.
     */
    void addRevision(@NotNull PunishmentRevision revision);

    /**
     * @return Whether the punishment is permanent or not.
     */
    boolean isPermanent();

    /**
     * Sets revocation info for the punishment
     * sets it as a revoked/appealed punishment
     * @param revocationInfo the revocation info
     */
    void setRevokeInfo(@Nullable RevocationInfo revocationInfo);

    /**
     * Sets the duration of the punishment,
     * if set to ZERO or NEGATIVE, this punishment is treated as a permanent punishment.
     * @param duration the duration of the punishment
     */
    void setDuration(Duration duration);

    default boolean hasExpired() {
        if(this.isPermanent() || getExpiresAt() == null ) return false;
        Instant expiresAt = this.getExpiresAt();
        return !expiresAt.isAfter(Instant.now());
    }
    /**
     * Information about a punishment revocation.
     */
    interface RevocationInfo {
        /**
         * Gets the issuer who revoked the punishment.
         * @return the revoker
         */
        @NotNull PunishmentIssuer getRevoker();

        /**
         * Gets the timestamp when the punishment was revoked.
         * @return the revocation timestamp
         */
        @NotNull Instant getRevokedAt();

        /**
         * Gets the reason for revocation.
         * @return the revocation reason
         */
        @NotNull String getReason();
    }

    @NotNull TagResolver asTagResolver();

}
