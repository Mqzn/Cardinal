package eg.mqzen.cardinal.api.punishments;

/**
 * Represents a type of punishable entities in the system.
 * Each punishable type indicates the nature of the entity being punished,
 * such as a player's account or an IP address.
 */
public enum PunishableType {
    PLAYER(),

    IP_ADDRESS(),

    PUNISHMENT_ID();


    PunishableType() {
    }
}