package net.mineabyss.cardinal.api.punishments;

import net.mineabyss.cardinal.api.util.FutureOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Manager interface for handling player punishments in the system.
 *
 * <p>This interface provides asynchronous methods to retrieve active punishments
 * for players. All methods return {@link FutureOperation} to ensure non-blocking
 * operations when accessing punishment data from storage systems.</p>
 *
 * <p>Punishments are organized by player UUID and can be filtered by 
 * {@link PunishmentType}. The system maintains a history of punishments
 * and provides access to both current active punishments and the most recent
 * punishment records.</p>
 *
 * @since 1.0
 * @see Punishment
 * @see PunishmentType
 */
public interface PunishmentManager {

    /**
     * The default limit for punishment history fetching when no specific limit is provided.
     *
     * @see #getFullHistory(UUID)
     */
    int DEFAULT_HISTORY_FETCH_LIMIT = 100;

    /**
     * Creates and applies a new punishment to the specified player.
     *
     * @param type the {@link PunishmentType} to apply
     * @param target the target to be punished (e.g.: IP or Player account)
     * @param issuer the UUID of the staff member applying the punishment, or null for console
     * @param reason the reason for the punishment
     * @param duration the duration of the punishment {@link Duration}, or -1 for permanent.
     *
     * @return a {@link FutureOperation} containing the created {@link Punishment}
     * @throws IllegalArgumentException if playerId, type, or reason is null
     */
    <T> Punishment<T> createPunishment(
            PunishmentType type,
            Punishable<T> target,
            PunishmentIssuer issuer,
            String reason,
            Duration duration
    );

    /**
     * Asynchronously applies a punishment to a target entity.
     *
     * <p>This method initiates a punishment action against a specified {@link Punishable}
     * target, which could be a player's account or an IP address. The punishment is issued
     * by a designated {@link PunishmentIssuer} and can be of a specific {@link PunishmentType}.
     *
     * <p>The operation is non-blocking and returns a {@link FutureOperation} that will
     * complete with the details of the applied {@link Punishment} once the action is finalized.
     *
     * @param <T>      The type of the punishable entity.
     * @param type     The type of punishment to apply (e.g., BAN, MUTE). Must not be {@code null}.
     * @param issuer   The entity issuing the punishment (e.g., a moderator, the system). Must not be {@code null}.
     * @param target   The entity to be punished, for example, a player's account or an IP address. Must not be {@code null}.
     * @param duration The duration of the punishment. A {@code null} value typically
     * signifies a permanent punishment.
     * @param reason   A descriptive reason for the punishment. May be {@code null} if no
     * reason is provided.
     * @return A {@link FutureOperation} that, upon completion, will hold the resulting
     * {@link Punishment}. This provides a way to asynchronously handle the
     * outcome of the punishment action.
     */
    <T> FutureOperation<Punishment<T>> applyPunishment(
            @NotNull PunishmentType type,
            @NotNull PunishmentIssuer issuer,
            @NotNull Punishable<T> target,
            @Nullable Duration duration,
            @Nullable String reason
    );

    /**
     Applies a punishment operation asynchronously.
     @param <T> the type of the punishment target or payload
     @param punishment the punishment to apply
     @return a future operation that completes with the applied punishment result
     @throws NullPointerException if punishment is null
     */
    <T> FutureOperation<Punishment<T>> applyPunishment(
            @NotNull Punishment<T> punishment
    );

    /**
     * Retrieves all active punishments for the specified player.
     *
     * <p>Returns a deque containing all currently active punishments for the player,
     * regardless of punishment type. The deque maintains insertion order, with the
     * most recent punishments typically appearing first.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @return a {@link FutureOperation} containing a {@link Deque} of active
     *         {@link Punishment} objects, or an empty deque if no active punishments exist
     * @throws IllegalArgumentException if playerId is null
     * @see #getActivePunishments(UUID, PunishmentType)
     */
    FutureOperation<Deque<Punishment<?>>> getActivePunishments(UUID playerId);

    /**
     * Retrieves all active punishments of a specific type for the specified player.
     *
     * <p>Filters active punishments by the specified {@link PunishmentType}.
     * This is useful when you need to check for specific punishment types like
     * bans, mutes, or kicks.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @param type the {@link PunishmentType} to filter by
     * @return a {@link FutureOperation} containing a {@link Deque} of active
     *         {@link Punishment} objects matching the specified type, or an empty
     *         deque if no matching active punishments exist
     * @throws IllegalArgumentException if playerId or type is null
     * @see #getActivePunishments(UUID)
     * @see #getActivePunishment(UUID, PunishmentType)
     */
    FutureOperation<Deque<Punishment<?>>> getActivePunishments(UUID playerId, PunishmentType type);

    /**
     * Retrieves a single active punishment of a specific type for the specified player.
     *
     * <p>Returns the first active punishment found of the specified type. If multiple
     * punishments of the same type are active, the behavior of which one is returned
     * depends on the implementation, but typically returns the most recent or severe one.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @param type the {@link PunishmentType} to search for
     * @return a {@link FutureOperation} containing an {@link Optional} with the
     *         {@link Punishment} if found, or {@link Optional#empty()} if no active
     *         punishment of the specified type exists
     * @throws IllegalArgumentException if playerId or type is null
     * @see #getActivePunishments(UUID, PunishmentType)
     * @see #getLastActivePunishment(UUID, PunishmentType)
     */
    FutureOperation<Optional<Punishment<?>>> getActivePunishment(UUID playerId, PunishmentType type);

    /**
     * Retrieves the active punishment for a specific IP address and punishment type.
     *
     * <p>This method converts the provided IP address to an internal representation
     * and then delegates to the standard punishment lookup mechanism. The IP address
     * is normalized and processed to create a consistent identifier that can be used
     * for punishment storage and retrieval.</p>
     *
     * @param ipAddress the IP address to check for active punishments. Must be a valid
     *                  IPv4 or IPv6 address string (e.g., "192.168.1.1" or "2001:db8::1")
     * @param type the type of punishment to search for (e.g., BAN, MUTE, KICK)
     * @return a {@link FutureOperation} that will complete with an {@link Optional} containing
     *         the active punishment if one exists, or an empty Optional if no active punishment
     *         of the specified type is found for the given IP address
     * @throws IllegalArgumentException if the ipAddress is null, empty, or not a valid IP address format
     * @throws NullPointerException if the punishment type is null
     * @see #getActivePunishment(UUID, PunishmentType)
     * @since 1.0
     */
    FutureOperation<Optional<Punishment<?>>> getActiveIPPunishment(String ipAddress, PunishmentType type);

    /**
     * Retrieves all active punishments for a specific IP address and punishment type.
     *
     * <p>This method converts the provided IP address to an internal representation
     * and then delegates to the standard punishment lookup mechanism. This is useful
     * for retrieving multiple active punishments of the same type that may exist for
     * an IP address (e.g., multiple temporary bans with different expiration times).</p>
     *
     * <p>The returned {@link Deque} maintains insertion order, with the most recently
     * applied punishments appearing last. If no active punishments are found, an
     * empty deque is returned.</p>
     *
     * @param ipAddress the IP address to check for active punishments. Must be a valid
     *                  IPv4 or IPv6 address string (e.g., "192.168.1.1" or "2001:db8::1")
     * @param type the type of punishment to search for (e.g., BAN, MUTE, KICK)
     * @return a {@link FutureOperation} that will complete with a {@link Deque} containing
     *         all active punishments of the specified type for the given IP address.
     *         The deque will be empty if no active punishments are found
     * @throws IllegalArgumentException if the ipAddress is null, empty, or not a valid IP address format
     * @throws NullPointerException if the punishment type is null
     * @see #getActivePunishments(UUID, PunishmentType)
     * @since 1.0
     */
    FutureOperation<Deque<Punishment<?>>> getActiveIPPunishments(String ipAddress, PunishmentType type);


    /**
     * Retrieves the most recent active punishment of a specific type for the specified player.
     *
     * <p>Returns the last (most recent) active punishment of the specified type.
     * This method is useful when you need to get the latest punishment applied
     * to a player for a specific violation type.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @param type the {@link PunishmentType} to search for
     * @return a {@link FutureOperation} containing an {@link Optional} with the
     *         most recent {@link Punishment} of the specified type if found, or
     *         {@link Optional#empty()} if no active punishment of the specified type exists
     * @throws IllegalArgumentException if playerId or type is null
     * @see #getActivePunishment(UUID, PunishmentType)
     * @see #getLastActivePunishment(UUID)
     */
    FutureOperation<Optional<Punishment<?>>> getLastActivePunishment(UUID playerId, PunishmentType type);

    /**
     * Retrieves the most recent active punishment of any type for the specified player.
     *
     * <p>Returns the last (most recent) active punishment regardless of type.
     * This method is useful for getting the player's most recent punishment
     * without needing to specify a particular {@link PunishmentType}.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @return a {@link FutureOperation} containing an {@link Optional} with the
     *         most recent active {@link Punishment} if found, or {@link Optional#empty()}
     *         if no active punishments exist for the player
     * @throws IllegalArgumentException if playerId is null
     * @see #getLastActivePunishment(UUID, PunishmentType)
     * @see #getActivePunishments(UUID)
     */
    FutureOperation<Optional<Punishment<?>>> getLastActivePunishment(UUID playerId);

    /**
     * Retrieves the complete punishment history for the specified player with a limit.
     *
     * <p>Returns the full punishment history (both active and inactive/expired punishments)
     * for the player, limited to the specified number of records. The history is typically
     * ordered chronologically with the most recent punishments first.</p>
     *
     * <p>The limit parameter controls how many punishment records are returned:
     * <ul>
     * <li>Positive values: Returns up to that many punishment records</li>
     * <li>-1: Returns all punishment records with no limit</li>
     * <li>0: Returns an empty deque</li>
     * </ul></p>
     *
     * @param playerId the UUID of the player to query punishment history for
     * @param limit the maximum number of punishment records to return, or -1 for no limit
     * @return a {@link FutureOperation} containing an {@link Optional} with a {@link Deque}
     *         of {@link Punishment} objects representing the player's history, or
     *         {@link Optional#empty()} if the player has no punishment history
     * @throws IllegalArgumentException if playerId is null or limit is negative (except -1)
     * @see #getFullHistory(UUID)
     * @see #getActivePunishments(UUID)
     */
    FutureOperation<Deque<Punishment<?>>> getFullHistory(UUID playerId, int limit);

    /**
     * Retrieves the complete punishment history for the specified player with the default limit.
     *
     * <p>This is a convenience method that calls {@link #getFullHistory(UUID, int)} with
     * the {@link #DEFAULT_HISTORY_FETCH_LIMIT} value of 100 records.</p>
     *
     * @param playerId the UUID of the player to query punishment history for
     * @return a {@link FutureOperation} containing an {@link Optional} with a {@link Deque}
     *         of {@link Punishment} objects representing the player's history (limited to
     *         {@value #DEFAULT_HISTORY_FETCH_LIMIT} records), or {@link Optional#empty()}
     *         if the player has no punishment history
     * @throws IllegalArgumentException if playerId is null
     * @see #getFullHistory(UUID, int)
     * @see #DEFAULT_HISTORY_FETCH_LIMIT
     */
    default FutureOperation<Deque<Punishment<?>>> getFullHistory(UUID playerId) {
        return getFullHistory(playerId, DEFAULT_HISTORY_FETCH_LIMIT);
    }



    /**
     * Revokes (removes) an active punishment by its ID.
     *
     * @param punishmentId the unique ID of the punishment to revoke
     * @param revoker the issuer of the revocation, it can not be null, if its automated or due to expiry of the time, then the CONSOLE would be
     *                the issuer of this revocation call.
     * @param reason the reason for revoking the punishment
     * @return a {@link FutureOperation} containing true if the punishment was successfully revoked, false if not found or already inactive
     * @throws IllegalArgumentException if punishmentId is null
     */
    FutureOperation<Boolean> revokePunishment(PunishmentID punishmentId, PunishmentIssuer revoker, String reason);

    /**
     * Updates the reason for an existing punishment.
     *
     * @param punishmentId the unique ID of the punishment to update
     * @param issuer the issuer requesting the updating of the punishment's reason.
     * @param newReason the new reason for the punishment
     *
     * @return a {@link FutureOperation} containing true if the punishment was successfully updated, false if not found
     * @throws IllegalArgumentException if punishmentId or newReason is null
     */
    FutureOperation<Boolean> updatePunishmentReason(PunishmentID punishmentId, PunishmentIssuer issuer, String newReason);

    /**
     * Checks if a player is currently banned.
     *
     * @param playerId the UUID of the player to check
     * @return a {@link FutureOperation} containing true if the player has an active ban, false otherwise
     * @throws IllegalArgumentException if playerId is null
     */
    FutureOperation<Boolean> isBanned(UUID playerId);

    /**
     * Checks if a player is currently muted.
     *
     * @param playerId the UUID of the player to check
     * @return a {@link FutureOperation} containing true if the player has an active mute, false otherwise
     * @throws IllegalArgumentException if playerId is null
     */
    FutureOperation<Boolean> isMuted(UUID playerId);

    /**
     * Checks if a player has any active punishment of the specified type.
     *
     * @param playerId the UUID of the player to check
     * @param type the {@link PunishmentType} to check for
     * @return a {@link FutureOperation} containing true if the player has an active punishment of the specified type, false otherwise
     * @throws IllegalArgumentException if playerId or type is null
     */
    FutureOperation<Boolean> hasActivePunishment(UUID playerId, PunishmentType type);

    // === Bulk Operations ===

    /**
     * Retrieves active punishments for multiple players at once.
     *
     * @param playerIds a collection of player UUIDs to query
     * @return a {@link FutureOperation} containing a map of player UUIDs to their active punishments
     * @throws IllegalArgumentException if playerIds is null or contains null values
     */
    FutureOperation<java.util.Map<UUID, Deque<Punishment<?>>>> getBulkActivePunishments(java.util.Collection<UUID> playerIds);

    /**
     * Revokes all active punishments of a specific type for a player.
     *
     * @param playerId the UUID of the player whose punishments should be revoked
     * @param type the {@link PunishmentType} to revoke, or null to revoke all types
     * @param revoker the UUID of the staff member revoking the punishments, or null for console
     * @param reason the reason for revoking the punishments
     * @return a {@link FutureOperation} containing the number of punishments that were revoked
     * @throws IllegalArgumentException if playerId is null
     */
    FutureOperation<Integer> revokeAllPunishments(UUID playerId, PunishmentType type, UUID revoker, String reason);



    // === Search & Query ===

    /**
     * Searches for punishments by punisher (staff member who applied them).
     *
     * @param issuer the issuer of the punishment.
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing a deque of punishments applied by the specified staff member
     * @throws IllegalArgumentException if punisherId is null
     */
    FutureOperation<Deque<Punishment<?>>> getPunishmentsByPunisher(PunishmentIssuer issuer, int limit);

    /**
     * Searches for punishments containing specific text in their reason.
     *
     * @param searchTerm the text to search for in punishment reasons
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing a deque of punishments with matching reasons
     * @throws IllegalArgumentException if searchTerm is null or empty
     */
    FutureOperation<Deque<Punishment<?>>> getPunishmentsByReason(String searchTerm, int limit);

    /**
     * Searches for a punishment record by its unique identifier.
     *
     * This method performs an asynchronous lookup of a punishment record using the provided
     * punishment ID. The operation returns a Future that will complete with the punishment
     * data once the search operation finishes.
     *
     * @param punishmentID the unique identifier of the punishment to search for.
     *                     Must not be null.
     * @return a Future that will complete with the PunishmentOperation result.
     *         The Future may complete with:
     *         - A successful result containing the punishment data if found
     *         - An empty result if no punishment with the given ID exists
     *         - An exceptional result if the search operation fails
     *
     * @throws IllegalArgumentException if punishmentID is null
     *
     * @since 1.0.0
     *
     * @apiNote This method is asynchronous and should be handled appropriately.
     *          Consider using CompletableFuture methods like thenAccept(),
     *          thenApply(), or exceptionally() to handle the result.
     *
     * @implNote The actual search implementation may involve database queries,
     *           file system operations, or network requests depending on the
     *           underlying storage mechanism.
     */
    FutureOperation<Optional<Punishment<?>>> getPunishmentByID(PunishmentID punishmentID);


    /**
     * Gets punishments that are set to expire within the specified time frame.
     *
     * @param duration the time frame in milliseconds to check for expiring punishments
     * @return a {@link FutureOperation} containing a deque of punishments expiring within the specified time
     * @throws IllegalArgumentException if withinMillis is negative
     */
    FutureOperation<Deque<Punishment<?>>> getExpiringPunishments(Duration duration);

    /**
     * Retrieves punishments issued within a specific time range.
     *
     * @param from the start time (inclusive)
     * @param to the end time (inclusive)
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments issued within the time range
     * @throws IllegalArgumentException if from or to is null, or from is after to
     */
    FutureOperation<Deque<Punishment<?>>> getPunishmentsIssuedBetween(Instant from, Instant to, int limit);

    /**
     * Retrieves punishments that expired within a specific time range.
     *
     * @param from the start time (inclusive)
     * @param to the end time (inclusive)
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments that expired within the time range
     * @throws IllegalArgumentException if from or to is null, or from is after to
     */
    FutureOperation<Deque<Punishment<?>>> getPunishmentsExpiredBetween(Instant from, Instant to, int limit);

    /**
     * Retrieves punishments issued in the last specified duration.
     *
     * @param duration the time period to look back from now
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing recent punishments
     * @throws IllegalArgumentException if duration is null or negative
     */
    FutureOperation<Deque<Punishment<?>>> getRecentPunishments(Duration duration, int limit);

    /**
     * Searches for punishments with multiple filter criteria.
     *
     * @param criteria the search criteria containing filters for type, issuer, target, etc.
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments matching the criteria
     * @throws IllegalArgumentException if criteria is null
     */
    FutureOperation<Deque<Punishment<?>>> searchPunishments(PunishmentSearchCriteria criteria, int limit);

    /**
     * Retrieves punishments by duration range (e.g., all 7-day bans).
     *
     * @param minDuration the minimum duration (inclusive), or null for no minimum
     * @param maxDuration the maximum duration (inclusive), or null for no maximum
     * @param type the punishment type to filter by, or null for all types
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments within the duration range
     */
    FutureOperation<Deque<Punishment<?>>> getPunishmentsByDuration(Duration minDuration, Duration maxDuration, PunishmentType type, int limit);

    /**
     * Retrieves all permanent punishments.
     *
     * @param type the punishment type to filter by, or null for all types
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing permanent punishments
     */
    FutureOperation<Deque<Punishment<?>>> getPermanentPunishments(PunishmentType type, int limit);

    /**
     * Gets the total count of punishments for a player.
     *
     * @param playerId the UUID of the player
     * @param type the punishment type to count, or null for all types
     * @param includeExpired whether to include expired/revoked punishments
     * @return a {@link FutureOperation} containing the punishment count
     * @throws IllegalArgumentException if playerId is null
     */
    FutureOperation<Integer> getPunishmentCount(UUID playerId, PunishmentType type, boolean includeExpired);

    /**
     * Gets punishment statistics for a player within a time period.
     *
     * @param playerId the UUID of the player
     * @param since the start time to count from
     * @return a {@link FutureOperation} containing punishment statistics
     * @throws IllegalArgumentException if playerId or since is null
     */
    FutureOperation<PunishmentStatistics> getPunishmentStatistics(UUID playerId, Instant since);

    /**
     * Gets the most frequently punished players.
     *
     * @param type the punishment type to analyze, or null for all types
     * @param limit the maximum number of players to return
     * @param since the time period to analyze from, or null for all time
     * @return a {@link FutureOperation} containing a map of player UUIDs to punishment counts
     */
    FutureOperation<LinkedHashMap<UUID, Integer>> getMostPunishedPlayers(PunishmentType type, int limit, Instant since);

    /**
     * Retrieves punishments by target type (useful for IP bans vs player bans).
     *
     * @param targetType the class of the target type (e.g., Player.class, IPAddress.class)
     * @param punishmentType the punishment type to filter by, or null for all types
     * @param activeOnly whether to include only active punishments
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments for the specified target type
     */
    <T> FutureOperation<Deque<Punishment<T>>> getPunishmentsByTargetType(Class<T> targetType, PunishmentType punishmentType, boolean activeOnly, int limit);

    /**
     * Finds related punishments (e.g., same IP address, related accounts).
     *
     * @param punishment the punishment to find related entries for
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing related punishments
     * @throws IllegalArgumentException if punishment is null
     */
    FutureOperation<Deque<Punishment<?>>> getRelatedPunishments(Punishment<?> punishment, int limit);

    /**
     * Retrieves punishments that have been appealed or modified.
     *
     * @param playerId the UUID of the player, or null for all players
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing appealed/modified punishments
     */
    FutureOperation<Deque<Punishment<?>>> getAppealedPunishments(UUID playerId, int limit);

    /**
     * Retrieves the revision history for a specific punishment.
     *
     * @param punishmentId the ID of the punishment
     * @return a {@link FutureOperation} containing the revision history
     * @throws IllegalArgumentException if punishmentId is null
     */
    FutureOperation<Deque<PunishmentRevision>> getPunishmentRevisions(PunishmentID punishmentId);

    void revokePunishmentFromMemory(Punishment<?> punishment);
}
