package eg.mqzen.cardinal.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IPUtils {

    /**
     * Converts an InetAddress to a UUID for storage/lookup purposes
     * @param inetAddress the InetAddress to convert
     * @return UUID representation of the IP address
     */
    public static UUID ipToUUID(InetAddress inetAddress) {
        if (inetAddress == null) {
            throw new IllegalArgumentException("InetAddress cannot be null");
        }

        return ipToUUID(inetAddress.getHostAddress());
    }

    /**
     * Converts an IP address to a reversible UUID using a custom encoding
     * This method creates UUIDs that can be reversed back to IP addresses
     * @param ipAddress the IP address to convert
     * @return reversible UUID representation of the IP address
     */
    public static UUID ipToUUID(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        String normalizedIP = normalizeIP(ipAddress);
        byte[] ipBytes = normalizedIP.getBytes(StandardCharsets.UTF_8);

        // Pad or truncate to 16 bytes for UUID
        byte[] uuidBytes = new byte[16];

        // First byte as a marker for IP-based UUIDs
        uuidBytes[0] = (byte) 0xFF;

        // Copy IP bytes
        int copyLength = Math.min(ipBytes.length, 15);
        System.arraycopy(ipBytes, 0, uuidBytes, 1, copyLength);

        // Create UUID from bytes
        long mostSigBits = 0;
        long leastSigBits = 0;

        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (uuidBytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (uuidBytes[i] & 0xff);
        }

        return new UUID(mostSigBits, leastSigBits);
    }



    /**
     * Extracts the original IP address from a reversible UUID created by ipToReversibleUUID
     * @param uuid the UUID to reverse
     * @return the original IP address if it's a reversible IP UUID, null otherwise
     */
    public static String uuidToIP(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        // Convert UUID back to bytes
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        byte[] uuidBytes = new byte[16];

        for (int i = 0; i < 8; i++) {
            uuidBytes[i] = (byte) (mostSigBits >> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            uuidBytes[i] = (byte) (leastSigBits >> (8 * (15 - i)));
        }

        // Check if this is an IP-based UUID (first byte should be 0xFF)
        if (uuidBytes[0] != (byte) 0xFF) {
            return null;
        }

        // Find the end of the IP string (look for null bytes)
        int endIndex = 16;
        for (int i = 1; i < 16; i++) {
            if (uuidBytes[i] == 0) {
                endIndex = i;
                break;
            }
        }

        // Extract IP bytes (skip the first marker byte)
        byte[] ipBytes = new byte[endIndex - 1];
        System.arraycopy(uuidBytes, 1, ipBytes, 0, endIndex - 1);

        return new String(ipBytes, StandardCharsets.UTF_8);
    }

    /**
     * Validates if an IP address string is valid
     * @param ipAddress the IP address to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIP(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }

        try {
            InetAddress.getByName(ipAddress.trim());
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Normalizes an IP address string (trims whitespace, handles IPv6 formatting)
     * @param ipAddress the IP address to normalize
     * @return normalized IP address
     * @throws IllegalArgumentException if IP is invalid
     */
    public static String normalizeIP(String ipAddress) {
        if (!isValidIP(ipAddress)) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
        }

        try {
            InetAddress inet = InetAddress.getByName(ipAddress.trim());
            return inet.getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress, e);
        }
    }

}

