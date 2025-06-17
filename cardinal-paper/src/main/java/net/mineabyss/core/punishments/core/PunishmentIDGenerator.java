package net.mineabyss.core.punishments.core;

import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@UtilityClass
public final class PunishmentIDGenerator {
    private static final Pattern PUNISHMENT_ID_PATTERN = Pattern.compile("^[0-9A-F]{8}$");
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
        return id != null && PUNISHMENT_ID_PATTERN.matcher(id).matches();
    }
}
