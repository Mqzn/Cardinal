package net.mineabyss.core.punishments;

import static net.mineabyss.core.punishments.StandardPunishmentManager.PUNISHMENT_TYPE_WRAP;

import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentID;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.PunishmentRevision;
import net.mineabyss.cardinal.api.punishments.PunishmentSearchCriteria;
import net.mineabyss.cardinal.api.punishments.PunishmentStatistics;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.punishments.templates.PunishmentHistoryService;
import net.mineabyss.cardinal.api.punishments.templates.TemplateId;
import net.mineabyss.cardinal.api.storage.QueryBuilder;
import net.mineabyss.cardinal.api.storage.Repository;
import net.mineabyss.cardinal.api.storage.StorageEngine;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.util.FutureOperation;
import org.apache.commons.lang3.Validate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StandardPunishmentHistoryService implements PunishmentHistoryService {

    private final StandardPunishmentManager manager;
    private final StorageEngine engine;
    public StandardPunishmentHistoryService(StandardPunishmentManager manager) {
        this.manager = manager;
        this.engine = manager.getEngine();
    }


    @Override
    public FutureOperation<List<Punishment<?>>> getPunishmentHistory(Punishable<?> target, TemplateId templateId) {
        return null;
    }

    /**
     * Searches for punishments by punisher (staff member who applied them).
     *
     * @param issuer the issuer of the punishment.
     * @param limit  the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing a deque of punishments applied by the specified staff member
     * @throws IllegalArgumentException if punisherId is null
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getPunishmentsByPunisher(PunishmentIssuer issuer, int limit) {

        return FutureOperation.of(
                CompletableFuture.supplyAsync(()-> {

                    Deque<Punishment<?>> punishments = new ArrayDeque<>();
                    for(var repo : manager.getPunishmentRepositories()) {
                        try {
                            punishments.addAll(
                                    repo.query().where("issuer").eq(issuer.getUniqueId().toString()).execute()
                            );
                        } catch (StorageException e) {
                            e.printStackTrace();
                            return punishments;
                        }
                    }

                    if(limit == -1) {
                        //permanent
                        return punishments;
                    }else {
                        Deque<Punishment<?>> trimmedPunishments = new ArrayDeque<>();
                        for (int i = 0; i < limit; i++) {
                            trimmedPunishments.addAll(punishments);
                        }
                        return trimmedPunishments;
                    }

                })
        );
    }

    /**
     * Searches for punishments containing specific text in their reason.
     *
     * @param searchTerm the text to search for in punishment reasons
     * @param limit      the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing a deque of punishments with matching reasons
     * @throws IllegalArgumentException if searchTerm is null or empty
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getPunishmentsByReason(String searchTerm, int limit) {

        return FutureOperation.of(
                CompletableFuture.supplyAsync(()-> {

                    Deque<Punishment<?>> punishments = new ArrayDeque<>();
                    for(var repo : manager.getPunishmentRepositories()) {
                        try {
                            punishments.addAll(repo.query().where("reason").like(searchTerm).execute());
                        } catch (StorageException e) {
                            e.printStackTrace();
                            return punishments;
                        }
                    }

                    if(limit == -1) {
                        //permanent
                        return punishments;
                    }else {
                        Deque<Punishment<?>> trimmedPunishments = new ArrayDeque<>();
                        for (int i = 0; i < limit; i++) {
                            trimmedPunishments.addAll(punishments);
                        }
                        return trimmedPunishments;
                    }

                })
        );
    }

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
     * The Future may complete with:
     * - A successful result containing the punishment data if found
     * - An empty result if no punishment with the given ID exists
     * - An exceptional result if the search operation fails
     * @throws IllegalArgumentException if punishmentID is null
     * @apiNote This method is asynchronous and should be handled appropriately.
     * Consider using CompletableFuture methods like thenAccept(),
     * thenApply(), or exceptionally() to handle the result.
     * @implNote The actual search implementation may involve database queries,
     * file system operations, or network requests depending on the
     * underlying storage mechanism.
     * @since 1.0.0
     */
    @Override
    public FutureOperation<Optional<Punishment<?>>> getPunishmentByID(PunishmentID punishmentID) {
        return FutureOperation.of(
                CompletableFuture.supplyAsync(()-> {

                    for(var repo : manager.getPunishmentRepositories()) {
                        try {
                            Optional<Punishment<?>> container = repo.query().where("id")
                                    .eq(punishmentID.getRepresentation()).findFirst();
                            if(container.isPresent()) {
                                return container;
                            }

                        } catch (StorageException e) {
                            e.printStackTrace();
                            continue;
                        }
                    }

                    return Optional.empty();
                })
        );
    }

    /**
     * Gets punishments that are set to expire within the specified time frame.
     *
     * @param duration the time frame in milliseconds to check for expiring punishments
     * @return a {@link FutureOperation} containing a deque of punishments expiring within the specified time
     * @throws IllegalArgumentException if withinMillis is negative
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getExpiringPunishments(Duration duration) {
        return FutureOperation.of(
                CompletableFuture.supplyAsync(()-> {
                    ArrayDeque<Punishment<?>> punishments = new ArrayDeque<>();

                    Instant now = Instant.now();
                    Instant windowEnd = now.plus(duration);

                    for(var repo : manager.getPunishmentRepositories()) {

                        try {
                            ArrayDeque<Punishment<?>> fields = new ArrayDeque<>(

                            );
                            punishments.addAll(
                                    repo.query()
                                            .where("expiresAt")
                                            .and()
                                            .gte(now)
                                            .lte(windowEnd)
                                            .sortBy("expiresAt", QueryBuilder.SortOrder.ASC)
                                            .execute()
                            );
                        } catch (StorageException e) {
                            throw new RuntimeException(e);
                        }

                    }

                    return punishments;
                })
        );

    }


    /**
     * Retrieves punishments that expired within a specific time range.
     *
     * @param from  the start time (inclusive)
     * @param to    the end time (inclusive)
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments that expired within the time range
     * @throws IllegalArgumentException if from or to is null, or from is after to
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getPunishmentsIssuedBetween(Instant from, Instant to, int limit) {
        return FutureOperation.of(
                engine.queryAcrossRepositories(PUNISHMENT_TYPE_WRAP)
                        .where("issuedAt")
                        .gte(from)
                        .and()
                        .where("issuedAt")
                        .lte(to)
                        .sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                        .limit(limit)
                        .executeAsync()
                        .thenApply(ArrayDeque::new));
    }

    /**
     * Retrieves punishments that expired within a specific time range.
     *
     * @param from  the start time (inclusive)
     * @param to    the end time (inclusive)
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments that expired within the time range
     * @throws IllegalArgumentException if from or to is null, or from is after to
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getPunishmentsExpiredBetween(Instant from, Instant to, int limit) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("From and to times cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From time cannot be after to time");
        }

        return FutureOperation.of(
                engine.queryAcrossRepositories(PUNISHMENT_TYPE_WRAP)
                        .where("expiresAt")
                        .gte(from)
                        .and()
                        .where("expiresAt")
                        .lte(to)
                        .sortBy("expiresAt", QueryBuilder.SortOrder.ASC)
                        .limit(limit)
                        .executeAsync()
                        .thenApply(ArrayDeque::new)
        );
    }

    /**
     * Retrieves punishments issued in the last specified duration.
     *
     * @param duration the time period to look back from now
     * @param limit    the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing recent punishments
     * @throws IllegalArgumentException if duration is null or negative
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getRecentPunishments(Duration duration, int limit) {
        if (duration == null) {
            throw new IllegalArgumentException("Duration cannot be null");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        Instant since = Instant.now().minus(duration);
        return getPunishmentsIssuedBetween(since, Instant.now(), limit);
    }

    /**
     * Searches for punishments with multiple filter criteria.
     *
     * @param criteria the search criteria containing filters for type, issuer, target, etc.
     * @param limit    the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments matching the criteria
     * @throws IllegalArgumentException if criteria is null
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> searchPunishments(PunishmentSearchCriteria criteria, int limit) {
        if (criteria == null) {
            throw new IllegalArgumentException("Criteria cannot be null");
        }

        QueryBuilder<Punishment<?>> query = engine.queryAcrossRepositories(PUNISHMENT_TYPE_WRAP);

        // Apply filters based on criteria
        if (criteria.getType() != null) {
            query.where("type").eq(criteria.getType().name());
        }
        if (criteria.getIssuer() != null) {
            query.where("issuer").eq(criteria.getIssuer().getUniqueId().toString());
        }
        if (criteria.getTargetPlayerId() != null) {
            query.where("target").eq(criteria.getTargetPlayerId().toString());
        }
        if (criteria.getReason() != null) {
            query.where("reason").like(criteria.getReason());
        }

        if (criteria.getIssuedAfter() != null) {
            query.where("issuedAt").gte(criteria.getIssuedAfter());
        }
        if (criteria.getIssuedBefore() != null) {
            query.where("issuedAt").lte(criteria.getIssuedBefore());
        }

        // Apply expiration filters
        if (criteria.getExpiresAfter() != null) {
            query.where("expiresAt").gte(criteria.getExpiresAfter());
        }
        if (criteria.getExpiresBefore() != null) {
            query.where("expiresAt").lte(criteria.getExpiresBefore());
        }

        // Apply duration filters
        if (criteria.getMinDuration() != null) {
            query.where("duration").gte(criteria.getMinDuration().toMillis());
        }
        if (criteria.getMaxDuration() != null) {
            query.where("duration").lte(criteria.getMaxDuration().toMillis());
        }

        return FutureOperation.of(
                query.sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                        .limit(limit)
                        .executeAsync()
                        .thenApply(ArrayDeque::new)
        );
    }

    /**
     * Retrieves punishments by duration range (e.g., all 7-day bans).
     *
     * @param minDuration the minimum duration (inclusive), or null for no minimum
     * @param maxDuration the maximum duration (inclusive), or null for no maximum
     * @param type        the punishment type to filter by, or null for all types
     * @param limit       the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments within the duration range
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getPunishmentsByDuration(
            Duration minDuration, Duration maxDuration,
            PunishmentType type, int limit
    ) {
        QueryBuilder<Punishment<?>> query = engine.queryAcrossRepositories(PUNISHMENT_TYPE_WRAP);

        // Apply type filter if specified
        if (type != null) {
            query.where("type").eq(type.name());
        }

        // Apply duration range filters
        if (minDuration != null) {
            query.where("duration").gte(minDuration.toMillis());
        }
        if (maxDuration != null) {
            query.where("duration").lte(maxDuration.toMillis());
        }

        return FutureOperation.of(
                query.sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                        .limit(limit)
                        .executeAsync()
                        .thenApply(ArrayDeque::new)
        );
    }

    /**
     * Retrieves all permanent punishments.
     *
     * @param type  the punishment type to filter by, or null for all types
     * @param limit the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing permanent punishments
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getPermanentPunishments(PunishmentType type, int limit) {
        QueryBuilder<Punishment<?>> query = engine.queryAcrossRepositories(PUNISHMENT_TYPE_WRAP)
                .where("duration").eq(-1); // -1 indicates permanent punishment

        // Apply type filter if specified
        if (type != null) {
            query.where("type").eq(type.name());
        }

        return FutureOperation.of(
                query.sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                        .limit(limit)
                        .executeAsync()
                        .thenApply(ArrayDeque::new)
        );
    }

    /**
     * Gets the total count of punishments for a player.
     *
     * @param playerId       the UUID of the player
     * @param type           the punishment type to count, or null for all types
     * @param includeExpired whether to include expired/revoked punishments
     * @return a {@link FutureOperation} containing the punishment count
     * @throws IllegalArgumentException if playerId is null
     */
    @Override
    public FutureOperation<Integer> getPunishmentCount(UUID playerId, PunishmentType type, boolean includeExpired) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }

        return FutureOperation.of(CompletableFuture.supplyAsync(() -> {
            int count = 0;
            for (Repository<String, Punishment<?>> repo : manager.getPunishmentRepositories()) {
                try {
                    QueryBuilder<Punishment<?>> query = repo.query()
                            .where("target").eq(playerId.toString());

                    // Apply type filter if specified
                    if (type != null) {
                        query.where("type").eq(type.name());
                    }

                    // Exclude expired punishments if requested
                    if (!includeExpired) {
                        query.where("expiresAt").gt(Instant.now());
                    }

                    count += (int) query.count();
                } catch (StorageException e) {
                    // Log error but continue processing other repositories
                }
            }
            return count;
        }));
    }

    /**
     * Gets punishment statistics for a player within a time period.
     *
     * @param playerId the UUID of the player
     * @param since    the start time to count from
     * @return a {@link FutureOperation} containing punishment statistics
     * @throws IllegalArgumentException if playerId or since is null
     */
    @Override
    public FutureOperation<PunishmentStatistics> getPunishmentStatistics(UUID playerId, Instant since) {
        Validate.notNull(playerId, "playerId cannot be null");
        Validate.notNull(since, "since cannot be null");

        return FutureOperation.of(CompletableFuture.supplyAsync(() -> {
            PunishmentStatistics.Builder builder = PunishmentStatistics.builder(playerId);
            Instant now = Instant.now();
            Instant firstPunishment = null;
            Instant lastPunishment = null;
            Instant mostRecentActive = null;

            for (Repository<String, Punishment<?>> repo : manager.getPunishmentRepositories()) {
                try {
                    QueryBuilder<Punishment<?>> query = repo.query()
                            .where("target").eq(playerId.toString())
                            .where("issuedAt").gte(since);

                    List<Punishment<?>> punishments = query.execute();

                    for (Punishment<?> punishment : punishments) {
                        // Update basic counts
                        builder.addPunishment(punishment.getType(), punishment.getDuration());

                        // Track first/last punishment times
                        Instant issuedAt = punishment.getIssuedAt();
                        if (firstPunishment == null || issuedAt.isBefore(firstPunishment)) {
                            firstPunishment = issuedAt;
                        }
                        if (lastPunishment == null || issuedAt.isAfter(lastPunishment)) {
                            lastPunishment = issuedAt;
                        }

                        // Track active punishments
                        if (punishment.getExpiresAt().isAfter(now)) {
                            if (mostRecentActive == null || issuedAt.isAfter(mostRecentActive)) {
                                mostRecentActive = issuedAt;
                            }
                        }

                        // Update issuer counts
                        builder.countByIssuer.merge(punishment.getIssuer(), 1, Integer::sum);
                    }
                } catch (StorageException e) {
                    // Log error but continue
                }
            }

            // Set the collected timestamps
            builder.firstPunishment(firstPunishment)
                    .lastPunishment(lastPunishment)
                    .mostRecentActive(mostRecentActive);

            return builder.build();
        }));
    }

    /**
     * Gets the most frequently punished players.
     *
     * @param type  the punishment type to analyze, or null for all types
     * @param limit the maximum number of players to return
     * @param since the time period to analyze from, or null for all time
     * @return a {@link FutureOperation} containing a map of player UUIDs to punishment counts
     */
    @Override
    public FutureOperation<LinkedHashMap<UUID, Integer>> getMostPunishedPlayers(
            PunishmentType type, int limit, Instant since
    ) {
        return FutureOperation.of(CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> counts = new HashMap<>();

            for (Repository<String, Punishment<?>> repo : manager.getPunishmentRepositories()) {
                try {
                    QueryBuilder<Punishment<?>> query = repo.query();

                    // Apply type filter if specified
                    if (type != null) {
                        query.where("type").eq(type.name());
                    }

                    // Apply time filter if specified
                    if (since != null) {
                        query.where("issuedAt").gte(since);
                    }

                    List<Punishment<?>> punishments = query.execute();
                    for (Punishment<?> punishment : punishments) {
                        UUID playerId = punishment.getTarget().getTargetUUID();
                        counts.put(playerId, counts.getOrDefault(playerId, 0) + 1);
                    }
                } catch (StorageException e) {
                    // Log error but continue processing other repositories
                }
            }

            // Sort by count descending and apply limit
            return counts.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        }));
    }

    /**
     * Retrieves punishments by target type (useful for IP bans vs player bans).
     *
     * @param targetType     the class of the target type (e.g., Player.class, IPAddress.class)
     * @param punishmentType the punishment type to filter by, or null for all types
     * @param activeOnly     whether to include only active punishments
     * @param limit          the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing punishments for the specified target type
     */
    @Override
    public <T> FutureOperation<Deque<Punishment<T>>> getPunishmentsByTargetType(
            Class<T> targetType,
            PunishmentType punishmentType,
            boolean activeOnly,
            int limit
    ) {
        return FutureOperation.of(CompletableFuture.supplyAsync(() -> {
            Deque<Punishment<T>> results = new ArrayDeque<>();

            for (Repository<String, Punishment<?>> repo : manager.getPunishmentRepositories()) {
                try {
                    QueryBuilder<Punishment<?>> query = repo.query()
                            .where("targetType").eq(targetType.getName());

                    // Apply punishment type filter if specified
                    if (punishmentType != null) {
                        query.where("type").eq(punishmentType.name());
                    }

                    // Filter active punishments if requested
                    if (activeOnly) {
                        query.where("expiresAt").gt(Instant.now());
                    }

                    List<Punishment<?>> punishments = query
                            .sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                            .limit(limit)
                            .execute();

                    for (Punishment<?> punishment : punishments) {
                        results.add((Punishment<T>) punishment);
                    }
                } catch (StorageException e) {
                    // Log error but continue processing other repositories
                }
            }

            return results;
        }));
    }

    /**
     * Finds related punishments (e.g., same IP address, related accounts).
     *
     * @param punishment the punishment to find related entries for
     * @param limit      the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing related punishments
     * @throws IllegalArgumentException if punishment is null
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getRelatedPunishments(Punishment<?> punishment, int limit) {
        if (punishment == null) {
            throw new IllegalArgumentException("Punishment cannot be null");
        }

        return FutureOperation.of(CompletableFuture.supplyAsync(() -> {
            Deque<Punishment<?>> results = new ArrayDeque<>();
            Object target = punishment.getTarget().getTarget();

            for (Repository<String, Punishment<?>> repo : manager.getPunishmentRepositories()) {
                try {
                    QueryBuilder<Punishment<?>> query = repo.query()
                            .where("target").eq(target.toString())
                            // Exclude the original punishment
                            .where("id").ne(punishment.getId().getRepresentation());

                    results.addAll(
                            query
                                    .sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                                    .limit(limit)
                                    .execute()
                    );
                } catch (StorageException e) {
                    // Log error but continue processing other repositories
                    e.printStackTrace();
                }
            }

            return results;
        }));
    }

    /**
     * Retrieves punishments that have been appealed or modified.
     *
     * @param playerId the UUID of the player, or null for all players
     * @param limit    the maximum number of results to return, or -1 for no limit
     * @return a {@link FutureOperation} containing appealed/modified punishments
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getAppealedPunishments(UUID playerId, int limit) {
        QueryBuilder<Punishment<?>> query = engine.queryAcrossRepositories(PUNISHMENT_TYPE_WRAP)
                .where("revisions"); // Assuming revisions field exists for appealed/modified punishments

        // Apply player filter if specified
        if (playerId != null) {
            query.where("target").eq(playerId.toString());
        }

        query.sortBy("issuedAt", QueryBuilder.SortOrder.DESC);

        if(limit > 0) {
            query.limit(limit);
        }

        return FutureOperation.of(
                query.executeAsync()
                        .thenApply(ArrayDeque::new)
        );
    }

    /**
     * Retrieves the revision history for a specific punishment.
     *
     * @param punishmentId the ID of the punishment
     * @return a {@link FutureOperation} containing the revision history
     * @throws IllegalArgumentException if punishmentId is null
     */
    @Override
    public FutureOperation<Deque<PunishmentRevision>> getPunishmentRevisions(PunishmentID punishmentId) {
        if (punishmentId == null) {
            throw new IllegalArgumentException("Punishment ID cannot be null");
        }

        return FutureOperation.of(CompletableFuture.supplyAsync(() -> {
            for (Repository<String, Punishment<?>> repo : manager.getPunishmentRepositories()) {
                try {
                    Optional<Punishment<?>> punishment = repo.findById(punishmentId.getRepresentation());
                    if (punishment.isPresent() && punishment.get().getRevisions() != null) {
                        return new ArrayDeque<>(punishment.get().getRevisions());
                    }
                } catch (StorageException e) {
                    // Log error but continue checking other repositories
                }
            }
            return new ArrayDeque<>();
        }));
    }

}
