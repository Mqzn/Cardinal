package eg.mqzen.cardinal.api.punishments;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a unique identifier for a punishment.
 * This interface provides methods to retrieve the unique ID and its string representation.
 */
public interface PunishmentID {

    /**
     * Returns a string representation of this punishment ID.
     *
     * @return the string representation
     */
    @NotNull String getRepresentation();

}
