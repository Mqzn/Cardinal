package net.mineabyss.core.punishments.target;

import net.kyori.adventure.text.Component;
import net.mineabyss.cardinal.api.CardinalProvider;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.PunishableType;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.PunishmentID;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.util.FutureOperation;
import net.mineabyss.core.punishments.core.StandardPunishmentID;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

final class PunishmentIDTarget implements Punishable<PunishmentID> {

    private final PunishmentID punishmentID;

    PunishmentIDTarget(String punishmentID) {
        this.punishmentID = new StandardPunishmentID(punishmentID);
    }


    @NotNull @Override
    public PunishableType getType() {
        return PunishableType.PUNISHMENT_ID;
    }

    @NotNull
    @Override
    public String getTargetName() {
        return punishmentID.getRepresentation();
    }

    @NotNull
    @Override
    public UUID getTargetUUID() {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");
    }

    @NotNull
    @Override
    public PunishmentID getTarget() {
        return punishmentID;
    }

    @Override
    public Instant getLastSeen() {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");

    }

    @Override
    public void refreshLastSeen() {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");
    }

    @Override
    public void kick(Component component) {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");
    }

    @Override
    public void sendMsg(String msg) {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");
    }

    @Override
    public void sendMsg(Component component) {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");
    }

    @Override
    public OfflinePlayer asOfflinePlayer() {
        throw new UnsupportedOperationException("Not allowed in PunishmentIDs");
    }

    @Override
    public FutureOperation<Optional<Punishment<?>>> fetchPunishment(PunishmentType punishmentType) {
        return CardinalProvider.provide().getPunishmentManager()
                .getPunishmentByID(this.punishmentID);
    }
}
