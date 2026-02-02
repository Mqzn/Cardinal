package eg.mqzen.cardinal.util;

import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eg.mqzen.cardinal.Cardinal;

import java.util.concurrent.CompletableFuture;

public final class UUIDFetcher {

    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final static LRUCache<String, UUID> cache = new LRUCache<>(DEFAULT_CACHE_SIZE);
    private final static HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    /**
     * Fetches UUID for a given username with LRU caching
     * @param username The Minecraft username
     * @return CompletableFuture containing the UUID, or null if player not found
     */
    public static CompletableFuture<UUID> fetchUUID(String username) {
        if (username == null || username.trim().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        String normalizedUsername = username.toLowerCase().trim();

        // Check cache first
        UUID cachedUUID = cache.get(normalizedUsername);
        if (cachedUUID != null) {
            return CompletableFuture.completedFuture(cachedUUID);
        }

        // Fetch from Mojang API
        return fetchFromAPI(normalizedUsername);
    }

    /**
     * Synchronous version of fetchUUID
     * @param username The Minecraft username
     * @return UUID or null if player not found
     */
    public static UUID fetchUUIDSync(String username) {
        try {
            return fetchUUID(username).join();
        } catch (Exception e) {
            Cardinal.warn("Failed to fetch UUID for name '" + username + "'");
            return null;
        }
    }

    private static CompletableFuture<UUID> fetchFromAPI(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOJANG_API_URL + username))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            String uuidString = json.get("id").getAsString();
                            UUID uuid = parseUUIDFromString(uuidString);

                            // Cache the result
                            cache.put(username, uuid);
                            return uuid;

                        } catch (Exception e) {
                            Cardinal.severe("Error parsing UUID response for " + username + ": " + e.getMessage());
                            return null;
                        }
                    } else if (response.statusCode() == 204) {
                        // Player not found - cache null to avoid repeated API calls
                        cache.put(username, null);
                        return null;
                    } else {
                        Cardinal.severe("API request failed for " + username + ". Status: " + response.statusCode());
                        return null;
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Exception during API request for " + username + ": " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Converts a UUID string without dashes to proper UUID format
     * @param uuidString UUID string from Mojang API (without dashes)
     * @return Properly formatted UUID
     */
    private static UUID parseUUIDFromString(String uuidString) {
        if (uuidString.length() == 32) {
            // Add dashes to create proper UUID format
            String formatted = uuidString.substring(0, 8) + "-" +
                    uuidString.substring(8, 12) + "-" +
                    uuidString.substring(12, 16) + "-" +
                    uuidString.substring(16, 20) + "-" +
                    uuidString.substring(20, 32);
            return UUID.fromString(formatted);
        } else if (uuidString.length() == 36) {
            return UUID.fromString(uuidString);
        } else {
            throw new IllegalArgumentException("Invalid UUID string format: " + uuidString);
        }
    }

    /**
     * Gets current cache size
     */
    public static int getCacheSize() {
        return cache.size();
    }

    /**
     * Clears the cache
     */
    public static void clearCache() {
        cache.clear();
    }

}