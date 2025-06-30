package com.mineabyss.cardinal.punishments.core;

import com.mineabyss.cardinal.api.punishments.PunishmentID;
import com.mineabyss.cardinal.util.PunishmentIDGenerator;
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
