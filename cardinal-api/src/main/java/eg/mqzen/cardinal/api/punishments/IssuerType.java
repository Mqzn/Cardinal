package eg.mqzen.cardinal.api.punishments;

/**
 * Represents the nature of the punishment's issuer/executor.
 * This enum defines two types of issuers:
 * - PLAYER: Indicates that the issuer is a player.
 * - CONSOLE: Indicates that the issuer is the console.
 */
public enum IssuerType {

    PLAYER, CONSOLE;

    public boolean isConsole() {
        return this == CONSOLE;
    }

    public boolean isPlayer() {
        return this == PLAYER;
    }

}