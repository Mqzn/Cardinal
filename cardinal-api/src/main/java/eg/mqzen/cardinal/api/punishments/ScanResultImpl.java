package eg.mqzen.cardinal.api.punishments;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

final class ScanResultImpl implements PunishmentScanResult {

    private final @Nullable Punishment<?> foundPunishment;
    private final @Nullable Throwable exception;

    ScanResultImpl(@Nullable Punishment<?> foundPunishment, @Nullable Throwable exception) {
        this.foundPunishment = foundPunishment;
        this.exception = exception;
    }


    @Override
    public Optional<Punishment<?>> getFoundPunishment() {
        return Optional.ofNullable(foundPunishment);
    }

    @Nullable
    @Override
    public Throwable getError() {
        return exception;
    }
}
