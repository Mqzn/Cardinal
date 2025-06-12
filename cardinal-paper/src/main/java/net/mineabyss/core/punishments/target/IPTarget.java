package net.mineabyss.core.punishments.target;

import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.PunishableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

final class IPTarget implements Punishable<String> {

    private final @Nullable PlayerTarget target;
    private final String ipAddress;
    private Instant lastSeen;

    IPTarget(@Nullable PlayerTarget target, String ipAddress) {
        this.target = target;
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the unique identifier for this punishable entity.
     *
     * @return the unique identifier
     */
    @Override
    public @NotNull PunishableType getType() {
        return PunishableType.IP_ADDRESS;
    }

    /**
     * Returns the name of the target entity.
     *
     * @return the name of the target entity
     */
    @NotNull
    @Override
    public String getTargetName() {
        if (target == null) {
            return ipAddress; // Fallback to IP address if target is null
        }
        return target.getTargetName();
    }

    /**
     * Returns the UUID of the target entity.
     *
     * @return the UUID of the target entity
     */
    @Override
    public @NotNull UUID getTargetUUID() {
        return UUID.nameUUIDFromBytes(ipAddress.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the target entity itself.
     *
     * @return the target entity
     */
    @NotNull @Override
    public String getTarget() {
        return ipAddress;
    }

    /**
     * Returns the last seen time of the target entity.
     *
     * @return the last seen time as an Instant
     */
    @Override
    public Instant getLastSeen() {
        return lastSeen; // Return the last seen time of the IP if target is null
    }

    /**
     * Sets last seen time of the target entity to the current time.
     */
    @Override
    public void refreshLastSeen() {
        lastSeen = Instant.now(); // Update last seen for the IP if target is null
    }

}
