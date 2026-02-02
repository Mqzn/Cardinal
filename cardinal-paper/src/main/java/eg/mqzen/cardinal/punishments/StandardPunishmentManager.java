package eg.mqzen.cardinal.punishments;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eg.mqzen.lib.commands.util.TypeUtility;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.lib.config.YamlDocument;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentID;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.PunishmentManager;
import eg.mqzen.cardinal.api.punishments.PunishmentScanResult;
import eg.mqzen.cardinal.api.punishments.PunishmentType;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.api.punishments.PunishmentHistoryService;
import eg.mqzen.cardinal.api.storage.Repository;
import eg.mqzen.cardinal.api.storage.StorageEngine;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.api.util.FutureOperation;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.punishments.core.StandardPunishment;
import eg.mqzen.cardinal.punishments.issuer.PunishmentIssuerFactory;
import eg.mqzen.cardinal.storage.StorageEngines;
import eg.mqzen.cardinal.util.IPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class StandardPunishmentManager implements PunishmentManager {

    final static TypeWrap<Punishment<?>> PUNISHMENT_TYPE_WRAP = new TypeWrap<Punishment<?>>() {};

    private final Cache<PunishmentType, PunishmentsCache> activePunishments = Caffeine.newBuilder()
            .maximumSize(StandardPunishmentType.values().length)
            .build();

    private final Cache<String, Punishment<?>> activePunishmentsPerID = Caffeine.newBuilder().build();

    private final ReentrantLock lock = new ReentrantLock();

    private final StorageEngine engine;

    private final PunishmentHistoryService historyService;

    private StandardPunishmentManager(YamlDocument config) throws StorageException {

        engine = StorageEngines.createFromYaml(config);
        for(PunishmentType type : StandardPunishmentType.values()) {
            engine.getRepositoryOrCreate(type.id(), PUNISHMENT_TYPE_WRAP);
            activePunishments.put(type, new PunishmentsCache(type));
        }

        historyService = new StandardPunishmentHistoryService(this);
    }

    public static PunishmentManager createNew(YamlDocument config) throws StorageException {
        return new StandardPunishmentManager(config);
    }


    @Override
    public StorageEngine getEngine() {
        return engine;
    }

    @Override
    public Repository<String, Punishment<?>> getPunishmentRepo(PunishmentType type) {
        return engine.getRepositoryOrCreate(type.id(), PUNISHMENT_TYPE_WRAP);
    }

    @Override
    public Collection<? extends Repository<String, Punishment<?>>> getPunishmentRepositories() {
        return engine.getRepositories().stream().filter((repo)-> TypeUtility.areRelatedTypes(repo.getEntityType().getType(), PUNISHMENT_TYPE_WRAP.getType()))
                .map((repo)-> (Repository<String, Punishment<?>>)repo)
                .toList();
    }

    /**
     * Creates and applies a new punishment to the specified player.
     *
     * @param type     the {@link PunishmentType} to apply
     * @param target   the target to be punished (e.g.: IP or Player account)
     * @param issuer   the UUID of the staff member applying the punishment, or null for console
     * @param reason   the reason for the punishment
     * @param duration the duration of the punishment {@link Duration}, or -1 for permanent.
     * @return a {@link FutureOperation} containing the created {@link Punishment}
     * @throws IllegalArgumentException if playerId, type, or reason is null
     */
    @Override
    public synchronized <T> Punishment<T> createPunishment(
            PunishmentType type,
            Punishable<T> target,
            PunishmentIssuer issuer,
            String reason,
            Duration duration
    ) {
        return new StandardPunishment<>(type, target, issuer, duration, reason);
    }

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
     * @param type     The type of punishment to apply (e.g., BAN, MUTE). Must not be {@code null}.
     * @param issuer   The entity issuing the punishment (e.g., a moderator, the system). Must not be {@code null}.
     * @param target   The entity to be punished, for example, a player's account or an IP address. Must not be {@code null}.
     * @param duration The duration of the punishment. A {@code null} value typically
     *                 signifies a permanent punishment.
     * @param reason   A descriptive reason for the punishment. May be {@code null} if no
     *                 reason is provided.
     * @return A {@link FutureOperation} that, upon completion, will hold the resulting
     * {@link Punishment}. This provides a way to asynchronously handle the
     * outcome of the punishment action.
     */
    @Override
    public <T> FutureOperation<Punishment<T>> applyPunishment(
            @NotNull PunishmentType type,
            @NotNull PunishmentIssuer issuer,
            @NotNull Punishable<T> target,
            @Nullable Duration duration,
            @Nullable String reason
    ) {

        Punishment<T> punishment = createPunishment(type, target, issuer, reason, duration);
        return applyPunishment(punishment);
    }

    @Override
    public <T> FutureOperation<Punishment<T>> applyPunishment(
            @NotNull Punishment<T> punishment
    ) {

        //memory cache
        updateActivePunishment(punishment);

        //db cache
        CompletableFuture<Punishment<T>> future = CompletableFuture.supplyAsync(()-> {
            var repo = getPunishmentRepo(punishment.getType());
            try {
                return (Punishment<T>) repo.save(punishment);
            } catch (StorageException e) {
                e.printStackTrace();
                return punishment;
            } catch (ClassCastException ex) {
                ex.printStackTrace();
                return punishment;
            }
        });
        return FutureOperation.of(future);
    }


    /**
     * Retrieves all active punishments for the specified player.
     *
     * <p>Returns a deque containing all currently active punishments for the player,
     * regardless of punishment type. The deque maintains insertion order, with the
     * most recent punishments typically appearing first.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @return a {@link FutureOperation} containing a {@link Deque} of active
     * {@link Punishment} objects, or an empty deque if no active punishments exist
     * @throws IllegalArgumentException if playerId is null
     * @see #getActivePunishments(UUID, PunishmentType)
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getActivePunishments(UUID playerId) {
        return FutureOperation.of(CompletableFuture.supplyAsync(()-> {
            StandardPunishmentType[] types = StandardPunishmentType.values();
            Deque<Punishment<?>> collected = new ArrayDeque<>();
            for(PunishmentType type : types) {
                var punishmentsOfType = activePunishments.get(type, PunishmentsCache::new);
                if(punishmentsOfType == null) {
                    continue;
                }
                Deque<Punishment<?>> userPunishments = punishmentsOfType.getPunishments(playerId);
                collected.addAll(userPunishments);
            }
            return collected;
        }));
    }

    /**
     * Retrieves all active punishments of a specific type for the specified player.
     *
     * <p>Filters active punishments by the specified {@link PunishmentType}.
     * This is useful when you need to check for specific punishment types like
     * bans, mutes, or kicks.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @param type     the {@link PunishmentType} to filter by
     * @return a {@link FutureOperation} containing a {@link Deque} of active
     * {@link Punishment} objects matching the specified type, or an empty
     * deque if no matching active punishments exist
     * @throws IllegalArgumentException if playerId or type is null
     * @see #getActivePunishments(UUID)
     * @see #getActivePunishment(UUID, PunishmentType)
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getActivePunishments(UUID playerId, PunishmentType type) {
        return FutureOperation.of(CompletableFuture.supplyAsync(()-> {
            Deque<Punishment<?>> collected = new ArrayDeque<>();
            var punishmentsOfType = activePunishments.get(type, PunishmentsCache::new);
            if(punishmentsOfType == null) {
                return collected;
            }
            Deque<Punishment<?>> userPunishments = punishmentsOfType.getPunishments(playerId);
            collected.addAll(userPunishments);

            return collected;
        }));
    }

    /**
     * Retrieves a single active punishment of a specific type for the specified player.
     *
     * <p>Returns the first active punishment found of the specified type. If multiple
     * punishments of the same type are active, the behavior of which one is returned
     * depends on the implementation, but typically returns the most recent or severe one.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @param type     the {@link PunishmentType} to search for
     * @return a {@link FutureOperation} containing an {@link Optional} with the
     * {@link Punishment} if found, or {@link Optional#empty()} if no active
     * punishment of the specified type exists
     * @throws IllegalArgumentException if playerId or type is null
     * @see #getActivePunishments(UUID, PunishmentType)
     * @see #getLastActivePunishment(UUID, PunishmentType)
     */
    @Override
    public FutureOperation<Optional<Punishment<?>>> getActivePunishment(@NotNull UUID playerId, PunishmentType type) {
        PunishmentsCache punishmentsOfType = activePunishments.get(type, (k)-> new PunishmentsCache(type));
        if(punishmentsOfType != null) {
            Deque<Punishment<?>> userPunishments = punishmentsOfType.getPunishmentsIfPresent(playerId);
            if (userPunishments != null) {
                Optional<Punishment<?>> activePunishment = Optional.ofNullable(userPunishments.peekLast());
                if (activePunishment.isPresent()) {
                    if(activePunishment.get().isRevoked() || activePunishment.get().hasExpired()) {
                        removeActivePunishmentFromCache(activePunishment.get());
                    }
                    else {
                        Cardinal.log("Fetched %s's from cached entries.", playerId.toString());
                        return FutureOperation.completed(activePunishment);
                    }
                }
            }
        }

        return FutureOperation.of(
                CompletableFuture.supplyAsync(()-> {
                    try {
                        Cardinal.log("Trying to fetch it from DB !!");
                        Optional<Punishment<?>> loadedActivePunishment = getPunishmentRepo(type).query()
                                .where("target.type").eq("PLAYER")
                                .and()
                                .where("target.uuid").eq(playerId.toString())
                                .and()
                                .where("revoke-info").eq(null)
                                .limit(1)
                                .findFirst();

                        loadedActivePunishment.map((loadedPunishment)-> {
                            if(loadedPunishment.hasExpired() ) {
                                return null;
                            }
                            return loadedPunishment;
                        }).ifPresent(this::updateActivePunishment);

                        return loadedActivePunishment;
                    } catch (StorageException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                })
        );
    }

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
    @Override
    public FutureOperation<Optional<Punishment<?>>> getActiveIPPunishment(String ipAddress, PunishmentType type) {
        return getActivePunishment(IPUtils.ipToUUID(ipAddress),  type);
    }


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
    @Override
    public FutureOperation<Deque<Punishment<?>>> getActiveIPPunishments(String ipAddress, PunishmentType type) {
        return getActivePunishments(IPUtils.ipToUUID(ipAddress), type);
    }

    /**
     * Retrieves the most recent active punishment of a specific type for the specified player.
     *
     * <p>Returns the last (most recent) active punishment of the specified type.
     * This method is useful when you need to get the latest punishment applied
     * to a player for a specific violation type.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @param type     the {@link PunishmentType} to search for
     * @return a {@link FutureOperation} containing an {@link Optional} with the
     * most recent {@link Punishment} of the specified type if found, or
     * {@link Optional#empty()} if no active punishment of the specified type exists
     * @throws IllegalArgumentException if playerId or type is null
     * @see #getActivePunishment(UUID, PunishmentType)
     * @see #getLastActivePunishment(UUID)
     */
    @Override
    public FutureOperation<Optional<Punishment<?>>> getLastActivePunishment(UUID playerId, PunishmentType type) {
        return FutureOperation.of(CompletableFuture.supplyAsync(()-> {
            var punishmentsOfType = activePunishments.get(type, PunishmentsCache::new);
            if(punishmentsOfType == null) {
                return Optional.empty();
            }
            Deque<Punishment<?>> userPunishments = punishmentsOfType.getPunishments(playerId);
            return Optional.ofNullable(userPunishments.peekLast());
        }));
    }

    /**
     * Retrieves the most recent active punishment of any type for the specified player.
     *
     * <p>Returns the last (most recent) active punishment regardless of type.
     * This method is useful for getting the player's most recent punishment
     * without needing to specify a particular {@link PunishmentType}.</p>
     *
     * @param playerId the UUID of the player to query punishments for
     * @return a {@link FutureOperation} containing an {@link Optional} with the
     * most recent active {@link Punishment} if found, or {@link Optional#empty()}
     * if no active punishments exist for the player
     * @throws IllegalArgumentException if playerId is null
     * @see #getLastActivePunishment(UUID, PunishmentType)
     * @see #getActivePunishments(UUID)
     */
    @Override
    public FutureOperation<Optional<Punishment<?>>> getLastActivePunishment(UUID playerId) {
        return getActivePunishments(playerId)
                .map(
                        (queue)-> queue.stream().max(Comparator.comparing(Punishment::getIssuedAt))
                );
    }

    @Override
    public Optional<Punishment<?>> getActivePunishmentByID(PunishmentID punishmentID) {
        return Optional.ofNullable(activePunishmentsPerID.getIfPresent(punishmentID.getRepresentation()));
    }


    /**
     * Revokes (removes) an active punishment by its ID.
     *
     * @param punishmentId the unique ID of the punishment to revoke
     * @param revoker      the UUID of the staff member revoking the punishment, or null for console
     * @param reason       the reason for revoking the punishment
     * @return a {@link FutureOperation} containing true if the punishment was successfully revoked, false if not found or already inactive
     * @throws IllegalArgumentException if punishmentId is null
     */
    @Override
    public FutureOperation<Boolean> revokePunishment(PunishmentID punishmentId, PunishmentIssuer revoker, String reason) {

        Punishment<?> punishment = activePunishmentsPerID.getIfPresent(punishmentId.getRepresentation());
        if(punishment != null) {
            punishment.revoke(new StandardPunishment.StandardRevocationInfo(revoker,reason));
        }

        return FutureOperation.of(CompletableFuture.supplyAsync(()-> {

            if(punishment == null) {
                return false;
            }

            PunishmentType type = punishment.getType();
            try {
                return this.getPunishmentRepo(type).save(punishment) != null;
            } catch (StorageException e) {
                e.printStackTrace();
                return false;
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }

        }))
        .thenApply((revoked)-> {
            if(revoked) {
                removeActivePunishmentFromCache(punishment);
            }
            return revoked;
        });
    }

    private void removeActivePunishmentFromCache(Punishment<?> punishment) {

        activePunishmentsPerID.invalidate(punishment.getId().getRepresentation());

        activePunishments.asMap().computeIfPresent(punishment.getType(),(t, oldCache)-> {
            if(oldCache == null) {
                return null;
            }

           oldCache.removePunishment(punishment.getTarget().getTargetUUID(), punishment);
           return oldCache;
        });

    }

    /**
     * Updates the reason for an existing punishment.
     *
     * @param punishmentId the unique ID of the punishment to update
     * @param issuer       the issuer requesting the updating of the punishment's reason.
     * @param newReason    the new reason for the punishment
     * @return a {@link FutureOperation} containing true if the punishment was successfully updated, false if not found
     * @throws IllegalArgumentException if punishmentId or newReason is null
     */
    @Override
    public FutureOperation<Boolean> updatePunishmentReason(PunishmentID punishmentId, PunishmentIssuer issuer, @NotNull String newReason) {
        return FutureOperation.of(CompletableFuture.supplyAsync(()-> {

            Punishment<?> punishment = null;
            Repository<String, Punishment<?>> punishmentRepository = null;
            for(var repo : this.getPunishmentRepositories()) {
                punishmentRepository = repo;

                Optional<Punishment<?>> container = null;
                try {
                    container = punishmentRepository.findById(punishmentId.getRepresentation());
                } catch (StorageException e) {
                    e.printStackTrace();
                    return false;
                }
                if(container.isEmpty()) {
                    continue;
                }
                else {
                    punishment = container.get();
                    break;
                }
            }

            //TODO save the revisions.

            lock.lock();
            updateActivePunishment(punishment);
            lock.unlock();

            punishment.setReason(newReason);
            try {
                punishmentRepository.save(punishment);
            } catch (StorageException e) {
                return false;
            }

            return true;
        }));
    }

    /**
     * Checks if a player is currently banned.
     *
     * @param playerId the UUID of the player to check
     * @return a {@link FutureOperation} containing true if the player has an active ban, false otherwise
     * @throws IllegalArgumentException if playerId is null
     */
    @Override
    public FutureOperation<Boolean> isBanned(UUID playerId) {
        return hasActivePunishment(playerId, StandardPunishmentType.BAN);
    }

    /**
     * Checks if a player is currently muted.
     *
     * @param playerId the UUID of the player to check
     * @return a {@link FutureOperation} containing true if the player has an active mute, false otherwise
     * @throws IllegalArgumentException if playerId is null
     */
    @Override
    public FutureOperation<Boolean> isMuted(UUID playerId) {
        return hasActivePunishment(playerId, StandardPunishmentType.MUTE);
    }

    /**
     * Checks if a player has any active punishment of the specified type.
     *
     * @param playerId the UUID of the player to check
     * @param type     the {@link PunishmentType} to check for
     * @return a {@link FutureOperation} containing true if the player has an active punishment of the specified type, false otherwise
     * @throws IllegalArgumentException if playerId or type is null
     */
    @Override
    public FutureOperation<Boolean> hasActivePunishment(UUID playerId, PunishmentType type) {
        return getActivePunishment(playerId, type).map(Optional::isPresent);
    }

    /**
     * Retrieves active punishments for multiple players at once.
     *
     * @param playerIds a collection of player UUIDs to query
     * @return a {@link FutureOperation} containing a map of player UUIDs to their active punishments
     * @throws IllegalArgumentException if playerIds is null or contains null values
     */
    @Override
    public FutureOperation<Map<UUID, Deque<Punishment<?>>>> getBulkActivePunishments(Collection<UUID> playerIds) {
        // Validate input
        if (playerIds == null) {
            throw new IllegalArgumentException("playerIds cannot be null");
        }
        if (playerIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("playerIds cannot contain null values");
        }

        // Precompute punishment types to avoid repeated calls
        Set<PunishmentType> punishmentTypes = activePunishments.asMap().keySet();

        // Create a future for each player
        List<CompletableFuture<AbstractMap.SimpleEntry<UUID, Deque<Punishment<?>>>>> playerFutures = playerIds.stream()
                .map(playerId -> {
                    // Create futures for all punishment types in parallel
                    List<CompletableFuture<Deque<Punishment<?>>>> typeFutures = punishmentTypes.stream()
                            .map(type -> getActivePunishments(playerId, type).unwrap())
                            .collect(Collectors.toList());

                    // Combine all punishment types for this player
                    return CompletableFuture.allOf(typeFutures.toArray(new CompletableFuture[0]))
                            .thenApply(ignored -> {
                                Deque<Punishment<?>> combined = new ArrayDeque<>();
                                typeFutures.forEach(future -> combined.addAll(future.join()));
                                return new AbstractMap.SimpleEntry<>(playerId, combined);
                            });
                })
                .collect(Collectors.toList());

        // Combine all player results
        CompletableFuture<Map<UUID, Deque<Punishment<?>>>> resultFuture = CompletableFuture.allOf(
                        playerFutures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> playerFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );

        return FutureOperation.of(resultFuture);
    }
    /**
     * Revokes all active punishments of a specific type for a player.
     *
     * @param playerId the UUID of the player whose punishments should be revoked
     * @param type     the {@link PunishmentType} to revoke, or null to revoke all types
     * @param revoker  the UUID of the staff member revoking the punishments, or null for console
     * @param reason   the reason for revoking the punishments
     * @return a {@link FutureOperation} containing the number of punishments that were revoked
     * @throws IllegalArgumentException if playerId is null
     */
    @Override
    public FutureOperation<Integer> revokeAllPunishments(
            UUID playerId,
            PunishmentType type,
            UUID revoker,
            String reason
    ) {
        Repository<String, Punishment<?>> punishmentRepo = engine.getRepositoryOrCreate(type.id(), PUNISHMENT_TYPE_WRAP);
        return FutureOperation.of(
                punishmentRepo.query()
                .where("target")
                .eq(playerId.toString())
                .executeAsync()
                .thenApplyAsync((punishments -> {
                    int count = 0;
                    for(Punishment<?> punishment : punishments) {

                        boolean completedWithoutIssues =
                                revokePunishment(punishment.getId(),
                                        PunishmentIssuerFactory.fromUUID(revoker), reason).unwrap().join();

                        if(completedWithoutIssues) {
                            count++;
                        }
                    }
                    return count;
                }))
        );
    }



    @NotNull
    @Override
    public FutureOperation<PunishmentScanResult> scan(@NotNull UUID uuid, @Nullable String ipAddress, PunishmentType punishmentType) {

        return getActivePunishment(uuid,punishmentType)
                .thenCompose((punishmentContainer)-> {

                    if(punishmentContainer.isPresent()) {
                        return CompletableFuture.completedFuture(punishmentContainer);
                    }

                    return getActiveIPPunishment(ipAddress, punishmentType)
                            .unwrap();

                })
                .thenApply((punishmentContainer)-> {
                    if(punishmentContainer.isPresent()) {
                        return PunishmentScanResult.success(punishmentContainer.get());
                    }

                    return PunishmentScanResult.failure();
                })
                .onErrorAndReturn((ex)-> {
                    return PunishmentScanResult.failure(ex);
                });
    }

    @NotNull @Override
    public PunishmentHistoryService getHistoryService() {
        return historyService;
    }


    private void updateActivePunishment(Punishment<?> punishment) {
        if(!punishment.getType().isMemoryWorthy()) {
            return;
        }
        activePunishmentsPerID.put(punishment.getId().getRepresentation(), punishment);
        //update in the type

        UUID punishmentsOwner = punishment.getTarget().getTargetUUID();

        //update
        activePunishments.asMap().computeIfPresent(punishment.getType(), (type, oldCache) -> {
            if(oldCache == null) {
                oldCache = new PunishmentsCache(type);
                oldCache.addPunishment(punishmentsOwner, punishment);
                return oldCache;
            }

            oldCache.removePunishment(punishmentsOwner, punishment);
            oldCache.addPunishment(punishmentsOwner, punishment);
            return oldCache;
        });
    }
}
