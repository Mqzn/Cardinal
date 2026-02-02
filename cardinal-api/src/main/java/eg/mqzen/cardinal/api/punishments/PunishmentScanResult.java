package eg.mqzen.cardinal.api.punishments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/**
 * Represents the result of a punishment scanning operation, encapsulating both
 * successful results and potential errors that occurred during the scan process.
 *
 * <p>This interface provides a clean way to handle both successful punishment
 * detection and error scenarios without throwing exceptions during normal operation.
 *
 * @since 1.0
 */
public sealed interface PunishmentScanResult permits ScanResultImpl {

    /**
     * Returns the punishment found during the scan operation, if any.
     *
     * <p>The returned Optional will be:
     * <ul>
     * <li>Present and containing a {@link Punishment} if an active punishment was found</li>
     * <li>Empty if no active punishments were found for the scanned parameters</li>
     * <li>Empty if an error occurred during scanning (check {@link #getError()})</li>
     * </ul>
     *
     * @return an Optional containing the found punishment, or empty if none found or error occurred
     * @see #getError()
     */
    Optional<Punishment<?>> getFoundPunishment();

    /**
     * Returns any throwable that occurred during the punishment scanning process.
     *
     * <p>This method will return:
     * <ul>
     * <li>{@code null} if the scan completed successfully (regardless of whether a punishment was found)</li>
     * <li>A non-null Throwable if an error occurred during the scanning process</li>
     * </ul>
     *
     * <p>When an error is present, {@link #getFoundPunishment()} will typically return an empty Optional.
     *
     * @return the exception that occurred during scanning, or null if no error occurred
     * @see #getFoundPunishment()
     */
    @Nullable
    Throwable getError();

    default boolean isSuccess() {
        return getError() == null && getFoundPunishment().isPresent();
    }


    //successful result with found punishment as a scan result.
    static PunishmentScanResult success(@NotNull Punishment<?> punishment) {
        return new ScanResultImpl(punishment, null);
    }

    //failed with errors (automatic corresponding to unexpected exceptions)
    static PunishmentScanResult failure(@NotNull Throwable exception) {
        return new ScanResultImpl(null, exception);
    }

    //failed with no errors (manual failing mechanism)
    static PunishmentScanResult failure() {
        return new ScanResultImpl(null, null);
    }

    default boolean failed() {
        return getError() != null || getFoundPunishment().isEmpty();
    }

    default void log() {
        Throwable ex = getError();
        if(ex != null)
            ex.printStackTrace();
    }
}