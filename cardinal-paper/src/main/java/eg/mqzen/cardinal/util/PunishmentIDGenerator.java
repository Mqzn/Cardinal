package eg.mqzen.cardinal.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@UtilityClass
public final class PunishmentIDGenerator {
    private static final String ID_PREFIX = "#";
    private final static AtomicInteger counter = new AtomicInteger(0);

    public static String generateNewID() {
        long timestamp = System.currentTimeMillis(); // Keep milliseconds
        int count = counter.getAndIncrement() & 0xFFFF;

        return String.format("%04X", (int)timestamp & 0xFFFF) +
                String.format("%04X", count);
    }
    /**
     * Validates if a string could be a valid punishment ID.
     * This method only validates the format (8 uppercase hex characters).
     * Note: This cannot distinguish between actual punishment IDs and other 8-character hex strings.
     *
     * For stricter validation, you would need additional business logic to check:
     * - If the timestamp part falls within expected ranges
     * - If the counter part is reasonable
     * - Cross-reference with your actual generated IDs
     *
     * @param id the string to validate
     * @return true if the string matches the basic punishment ID format, false otherwise
     */
    public static boolean isValidPunishmentID(String id) {
        return id != null && id.startsWith(ID_PREFIX) && id.length() == 9 &&
                Pattern.matches(ID_PREFIX + "[0-9A-F]{8}", id);
    }
}
