package net.mineabyss.core.listener;

import net.kyori.adventure.text.Component;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import net.mineabyss.core.Cardinal;
import net.mineabyss.core.punishments.issuer.PunishmentIssuerFactory;
import net.mineabyss.core.util.PunishmentMessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import java.util.Optional;
import java.util.UUID;


public class BanListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String playerName = event.getName();

        try {
            // Check for active ban punishment
            Optional<Punishment<?>> activeBan = getActiveBanPunishment(uuid);

            if (activeBan.isEmpty()) {
                Cardinal.log("No active ban punishments!");
                return; // No active ban, allow login
            }

            Punishment<?> punishment = activeBan.get();
            LoginResult result = processBanPunishment(punishment, uuid, playerName);

            switch (result.action()) {
                case ALLOW -> {
                    if (result.message() != null) {
                        Cardinal.log("Player " + playerName + " ban expired/revoked: " + result.message());
                    }
                    event.allow();
                }
                case DENY -> {
                    Cardinal.log("Player " + playerName + " login denied - Active ban: " + punishment.getId().getRepresentation());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.kickMessage());
                }
                case ERROR -> {
                    Cardinal.severe("Error processing ban for player " + playerName + ": " + result.message());
                    // Fail-safe: deny login on error to prevent bypassing bans
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            Component.text("Authentication error. Please try again later."));
                }
            }

        } catch (Exception e) {
            Cardinal.severe("Unexpected error during ban check for player " + playerName + ": " + e.getMessage());
            // Fail-safe: allow login on unexpected errors (configurable behavior)
            if (shouldDenyOnError()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("Server error. Please try again later."));
            }
        }
    }

    /**
     * Retrieves active ban punishment for the given UUID
     */
    private Optional<Punishment<?>> getActiveBanPunishment(UUID uuid) {
        try {
            return Cardinal.getInstance().getPunishmentManager()
                    .getActivePunishment(uuid, StandardPunishmentType.BAN)
                    .join();
        } catch (Exception e) {
            Cardinal.severe("Failed to retrieve active ban for UUID " + uuid + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Processes ban punishment and determines login result
     */
    private LoginResult processBanPunishment(Punishment<?> punishment, UUID uuid, String playerName) {
        try {
            // Handle permanent bans
            System.out.println("Punishment");
            if (punishment.isPermanent()) {
                return LoginResult.deny(PunishmentMessageUtil.getBanKickMessage(punishment));
            }

            // Handle temporary bans
            if (punishment.hasExpired()) {
                return handleExpiredBan(punishment, playerName);
            }
            // Ban is still active
            return LoginResult.deny(PunishmentMessageUtil.getBanKickMessage(punishment));

        } catch (Exception e) {
            return LoginResult.error("Failed to process ban punishment: " + e.getMessage());
        }
    }

    /**
     * Handles expired ban punishment
     */
    private LoginResult handleExpiredBan(Punishment<?> punishment, String playerName) {
        try {
            boolean revoked = Cardinal.getInstance().getPunishmentManager()
                    .revokePunishment(punishment.getId(),
                            PunishmentIssuerFactory.fromConsole(),
                            "EXPIRED")
                    .join();

            if (revoked) {
                return LoginResult.allow("Ban expired and revoked successfully");
            } else {
                Cardinal.warn("Failed to revoke expired ban for player " + playerName + ": " + punishment.getId().getRepresentation());
                return LoginResult.error("Failed to process expired ban");
            }

        } catch (Exception e) {
            Cardinal.severe("Error revoking expired ban for player " + playerName + ": " + e.getMessage());
            return LoginResult.error("Error processing expired ban: " + e.getMessage());
        }
    }

    /**
     * Configuration-based decision for error handling
     */
    private boolean shouldDenyOnError() {
        // This should be configurable - fail-safe vs fail-open
        return true; // Default to secure behavior
    }

    /**
     * Result class for login processing
     */
    private record LoginResult(BanListener.LoginResult.Action action, String message, Component kickMessage) {

        public enum Action {
            ALLOW, DENY, ERROR
        }

        public static LoginResult allow(String message) {
            return new LoginResult(Action.ALLOW, message, null);
        }

        public static LoginResult deny(Component kickMessage) {
            return new LoginResult(Action.DENY, null, kickMessage);
        }

        public static LoginResult error(String message) {
            return new LoginResult(Action.ERROR, message, null);
        }

    }
}
