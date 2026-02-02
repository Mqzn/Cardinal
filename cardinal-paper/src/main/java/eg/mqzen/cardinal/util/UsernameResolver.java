package eg.mqzen.cardinal.util;

import eg.mqzen.cardinal.Cardinal;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UsernameResolver {
    
    private static final ConcurrentHashMap<UUID, String> usernameCache = new ConcurrentHashMap<>();
    
    public static CompletableFuture<String> getUsernameFromUUID(UUID uuid) {
        // Check cache first
        String cached = usernameCache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String strippedUUID = uuid.toString().replace("-", "");
                URL url = URI.create("https://api.mojang.com/user/profile/" + strippedUUID).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == 200) {
                    JSONObject response = (JSONObject) new JSONParser().parse(
                        new InputStreamReader(connection.getInputStream())
                    );
                    String username = (String) response.get("getName");
                    
                    // Cache result for 1 hour
                    usernameCache.put(uuid, username);
                    Tasks.runAsyncLater(() ->
                        usernameCache.remove(uuid), TimeUnit.HOURS.toMinutes(1) * 60 * 20);
                    
                    return username;
                }
                throw new RuntimeException("Player not found (HTTP " + connection.getResponseCode() + ")");
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch username: " + e.getMessage());
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(Cardinal.getInstance(), runnable));
    }
}