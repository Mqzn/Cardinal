package net.mineabyss.core.punishments;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mineabyss.lib.commands.util.TypeUtility;
import com.mineabyss.lib.commands.util.TypeWrap;
import com.mineabyss.lib.config.YamlDocument;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentID;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.PunishmentManager;
import net.mineabyss.cardinal.api.punishments.PunishmentRevision;
import net.mineabyss.cardinal.api.punishments.PunishmentSearchCriteria;
import net.mineabyss.cardinal.api.punishments.PunishmentStatistics;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.cardinal.api.storage.QueryBuilder;
import net.mineabyss.cardinal.api.storage.Repository;
import net.mineabyss.cardinal.api.storage.StorageEngine;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.cardinal.api.util.FutureOperation;
import net.mineabyss.core.punishments.core.StandardPunishment;
import net.mineabyss.core.punishments.issuer.PunishmentIssuerFactory;
import net.mineabyss.core.storage.StorageEngines;
import net.mineabyss.core.util.IPUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private final static TypeWrap<Punishment<?>> PUNISHMENT_TYPE_WRAP = new TypeWrap<Punishment<?>>() {};

    private final Cache<PunishmentType, PunishmentsCache> activePunishments = Caffeine.newBuilder()
            .maximumSize(StandardPunishmentType.values().length)
            .build();

    private final Cache<String, Punishment<?>> activePunishmentsPerID = Caffeine.newBuilder().build();

    private final ReentrantLock lock = new ReentrantLock();

    private final StorageEngine engine;

    private StandardPunishmentManager(YamlDocument config) throws StorageException {

        engine = StorageEngines.createFromYaml(config);
        for(PunishmentType type : StandardPunishmentType.values()) {
            engine.getRepositoryOrCreate(type.id(), PUNISHMENT_TYPE_WRAP);
            activePunishments.put(type, new PunishmentsCache(type));
        }
    }

    public static PunishmentManager createNew(YamlDocument config) throws StorageException {
        return new StandardPunishmentManager(config);
    }


    private Repository<String, Punishment<?>> getPunishmentRepo(PunishmentType type) {
        return engine.getRepositoryOrCreate(type.id(), PUNISHMENT_TYPE_WRAP);
    }
    
    private Collection<? extends Repository<String, Punishment<?>>> getPunishmentRepositories() {
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
            System.out.println("Found punishment cache for type " + type.name());
            Deque<Punishment<?>> userPunishments = punishmentsOfType.getPunishmentsIfPresent(playerId);
            if (userPunishments != null) {
                System.out.println("Found punishment deque for type " + type.name());
                Optional<Punishment<?>> activePunishment = Optional.ofNullable(userPunishments.getLast());
                if (activePunishment.isPresent()) {
                    System.out.println("Found active punishment in the deque");
                    return FutureOperation.of(CompletableFuture.completedFuture(activePunishment));
                }
            }
        }else {
            System.out.println("NO PUNISHMENT CACHE OF TYPE " + type.name());
        }
        return FutureOperation.of(
                CompletableFuture.supplyAsync(()-> {
                    try {
                        System.out.println("Trying to fetch it from DB !!");
                        Optional<Punishment<?>> loadedActivePunishment = getPunishmentRepo(type).query()
                                .where("target.type").eq("PLAYER")
                                .and()
                                .where("target.uuid").eq(playerId.toString())
                                .and()
                                .where("expiresAt").gt(Instant.now().toEpochMilli())
                                .limit(1)
                                .findFirst();

                        loadedActivePunishment.ifPresent(this::updateActivePunishment);
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
     * @param limit    the maximum number of punishment records to return, or -1 for no limit
     * @return a {@link FutureOperation} containing an {@link Optional} with a {@link Deque}
     * of {@link Punishment} objects representing the player's history, or
     * {@link Optional#empty()} if the player has no punishment history
     * @throws IllegalArgumentException if playerId is null or limit is negative (except -1)
     * @see #getFullHistory(UUID)
     * @see #getActivePunishments(UUID)
     */
    @Override
    public FutureOperation<Deque<Punishment<?>>> getFullHistory(UUID playerId, int limit) {
        return FutureOperation.of(CompletableFuture.supplyAsync(()-> {
            Deque<Punishment<?>> fullHistory = new ArrayDeque<>();

            for (var punishmentRepo : this.getPunishmentRepositories()) {
                try {
                    punishmentRepo.findAll().forEach(fullHistory::add);
                } catch (StorageException e) {
                    throw new RuntimeException(e);
                }

            }
            if(limit == -1) {
                return fullHistory;
            }

            Deque<Punishment<?>> trimmedHistory = new ArrayDeque<>();
            for (int i = 0; i < limit; i++) {
                Punishment<?> punishment = fullHistory.poll();
                if(punishment == null || fullHistory.isEmpty()) break;
                trimmedHistory.add(punishment);
            }
            return trimmedHistory;
        }));
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
                    for(var repo : this.getPunishmentRepositories()) {
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
                    for(var repo : this.getPunishmentRepositories()) {
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

                    Punishment<?> punishment = null;
                    for(var repo : this.getPunishmentRepositories()) {

                        try {
                            punishment = repo.query().where("id")
                            .eq(punishmentID.getRepresentation()).findFirst().get();
                        } catch (StorageException e) {
                            throw new RuntimeException(e);
                        }

                        if(punishment != null) {
                            return Optional.ofNullable(punishment);
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

                    for(var repo : this.getPunishmentRepositories()) {

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
            for (Repository<String, Punishment<?>> repo : this.getPunishmentRepositories()) {
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

                    count += query.count();
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

            for (Repository<String, Punishment<?>> repo : this.getPunishmentRepositories()) {
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

            for (Repository<String, Punishment<?>> repo : this.getPunishmentRepositories()) {
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

            for (Repository<String, Punishment<?>> repo : this.getPunishmentRepositories()) {
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

            for (Repository<String, Punishment<?>> repo : this.getPunishmentRepositories()) {
                try {
                    QueryBuilder<Punishment<?>> query = repo.query()
                            .where("target").eq(target.toString());

                    // Exclude the original punishment
                    if (punishment.getId() != null) {
                        query.where("id").ne(punishment.getId().getRepresentation());
                    }

                    results.addAll(
                            query
                            .sortBy("issuedAt", QueryBuilder.SortOrder.DESC)
                            .limit(limit)
                            .execute()
                    );
                } catch (StorageException e) {
                    // Log error but continue processing other repositories
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
            for (Repository<String, Punishment<?>> repo : this.getPunishmentRepositories()) {
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

    private void updateActivePunishment(Punishment<?> punishment) {
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
