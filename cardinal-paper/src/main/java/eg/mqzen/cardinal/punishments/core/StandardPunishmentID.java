package eg.mqzen.cardinal.punishments.core;

import eg.mqzen.cardinal.api.punishments.PunishmentID;
import eg.mqzen.cardinal.util.PunishmentIDGenerator;
import org.jetbrains.annotations.NotNull;

public final class StandardPunishmentID implements PunishmentID {

    private final String representation;

    public StandardPunishmentID(String representation) {
        this.representation = representation;
    }

    public StandardPunishmentID() {
        this.representation = PunishmentIDGenerator.generateNewID();
    }

    /**
     * Returns a string representation of this punishment ID.
     *
     * @return the string representation
     */
    @Override
    public @NotNull String getRepresentation() {
        return representation;
    }
}
