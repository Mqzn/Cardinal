package eg.mqzen.cardinal.punishments.target;

import net.kyori.adventure.text.Component;
import eg.mqzen.cardinal.api.CardinalProvider;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.PunishableType;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentID;
import eg.mqzen.cardinal.api.punishments.PunishmentType;
import eg.mqzen.cardinal.api.util.FutureOperation;
import eg.mqzen.cardinal.punishments.core.StandardPunishmentID;
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
                .getHistoryService()
                .getPunishmentByID(this.punishmentID, punishmentType);
    }
}
