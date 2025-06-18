package net.mineabyss.cardinal.api.punishments.templates;

import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentID;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.PunishmentRevision;
import net.mineabyss.cardinal.api.punishments.PunishmentSearchCriteria;
import net.mineabyss.cardinal.api.punishments.PunishmentStatistics;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.util.FutureOperation;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PunishmentHistoryService {
    FutureOperation<List<Punishment<?>>> getPunishmentHistory(Punishable<?> target, TemplateId templateId);

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
     * <p>
     * This method performs an asynchronous lookup of a punishment record using the provided
     * punishment ID. The operation returns a Future that will complete with the punishment
     * data once the search operation finishes.
     *
     * @param punishmentID the unique identifier of the punishment to search for.
     *                     Must not be null.
     * @return a Future that will complete with the PunishmentOperation result.
     *         The Future may be complete with:
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

}