package eg.mqzen.cardinal.api.punishments;

/**
 * Represents a type of punishment in the system.
 * Each punishment type has a unique identifier.
 */
public interface PunishmentType {

    /**
     * Returns the unique identifier for this punishment type.
     * @return the unique identifier as a string
     */
    String id();

    /**
     * Full name may be uppercase if the implementation is an enum
     * @return the full name
     */
    String name();

    /**
     * @return Whether the punishment of that type shall be cached in memory or not.
     */
    boolean isMemoryWorthy();

    boolean supportsDuration();
}
