package eg.mqzen.cardinal.api.punishments;

import net.kyori.adventure.text.Component;
import eg.mqzen.cardinal.api.util.FutureOperation;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an entity that can be punished.
 * This interface provides methods to retrieve the type, target name, UUID, and last seen time of the punishable entity.
 *
 * @param <T> the type of the target entity (e.g., Player, IPAddress)
 */
public interface Punishable<T> {

    /**
     * Returns the unique identifier for this punishable entity.
     *
     * @return the unique identifier
     */
    @NotNull PunishableType getType();

    /**
     * Returns the name of the target entity.
     *
     * @return the name of the target entity
     */
    @NotNull String getTargetName();

    /**
     * Returns the UUID of the target entity.
     *
     * @return the UUID of the target entity
     */
    @NotNull UUID getTargetUUID();

    /**
     * Returns the target entity itself.
     *
     * @return the target entity
     */
    @NotNull T getTarget();

    /**
     * Returns the last seen time of the target entity.
     *
     * @return the last seen time as an Instant
     */
    Instant getLastSeen();

    /**
     * Sets last seen time of the target entity to the current time.
     */
    void refreshLastSeen();

    void kick(Component component);

    void sendMsg(String msg);

    void sendMsg(Component component);

    OfflinePlayer asOfflinePlayer();

    FutureOperation<Optional<Punishment<?>>> fetchPunishment(PunishmentType punishmentType);
}
