package eg.mqzen.cardinal.config;

import eg.mqzen.cardinal.api.config.MessageKey;

/**
 * Central registry for all message keys with hierarchical organization
 */
public final class MessageKeys {
    private MessageKeys() {} // Utility class

    // Root level messages
    public static final MessageKey PREFIX = SimpleMessageKey.of(
            "prefix",
            "<green>Plugin has been enabled!"
    );

    public static final MessageKey PRIMARY_COLOR = SimpleMessageKey.of(
            "primary_color",
            "<gray>"
    );

    public static final MessageKey SECONDARY_COLOR = SimpleMessageKey.of(
            "secondary_color",
            "<red>"
    );


    // Punishment system messages
    public static final class Punishments extends MessageKeyContainer {
        private static final Punishments ROOT = new Punishments();

        private Punishments() {
            super("punishments", null);
        }

        // Ban related messages
        public static final class Ban extends MessageKeyContainer {
            private static final Ban INSTANCE = new Ban();

            private Ban() {
                super("ban", ROOT);
            }

            public static final MessageKey ALREADY_BANNED = INSTANCE.createKey("already_banned", "<prefix> <dark_red>ERROR:</dark_red> <red>User "
                    + "<target> "
                    + "is already banned!");

            public static final MessageKey KICK_MESSAGE_PERMANENT = INSTANCE.createKey(
                    "kick_message_permanent",
                    "<gradient:#ff4444:#cc0000><bold>⚠ SERVER BAN NOTICE ⚠</bold></gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<red><bold>You have been banned from this server!</bold></red>\n" +
                            "<gray>📋 </gray><white>Punishment ID:</white> <yellow>#<punishment_id></yellow>\n" +
                            "<gray>📝 </gray><white>Reason:</white> <#ffa500><punishment_reason></#ffa500>\n" +
                            "<gray>📅 </gray><white>Issued:</white> <aqua><punishment_issued_date></aqua>\n" +
                            "<gray>♾️ </gray><gradient:#ff6b6b:#cc0000>This ban will never expire!</gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray><italic>Contact staff if you believe this is an error</italic></gray>"
            );

            public static final MessageKey KICK_MESSAGE_TEMPORARY = INSTANCE.createKey(
                    "kick_message_temporary",
                    "<gradient:#ff4444:#cc0000><bold>⚠ SERVER BAN NOTICE ⚠</bold></gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<red><bold>You have been banned from this server!</bold></red>\n" +
                            "<gray>📋 </gray><white>Punishment ID:</white> <yellow>#<punishment_id></yellow>\n" +
                            "<gray>📝 </gray><white>Reason:</white> <#ffa500><punishment_reason></#ffa500>\n" +
                            "<gray>📅 </gray><white>Issued:</white> <aqua><punishment_issued_date></aqua>\n" +
                            "<gray>⏰ </gray><white>Expires:</white> <green><punishment_expires_date></green>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray><italic>Contact staff if you believe this is an error</italic></gray>"
            );

            public static final MessageKey BROADCAST = INSTANCE.createKey(
                    "broadcast",
                    "<red>⚡ <bold><punishment_target></bold> <gray>has been <red><bold>banned</bold></red> <dark_gray>» "
                            + "<yellow><punishment_reason></yellow> <dark_gray>by <aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_SILENT = INSTANCE.createKey(
                    "broadcast_silent",
                    "<dark_gray>[<gray>SILENT<dark_gray>]</gray> <red>⚡ <bold><punishment_target></bold> <gray>has been "
                            + "<red><bold>banned</bold></red> <dark_gray>» <yellow><punishment_reason></yellow> <dark_gray>by "
                            + "<aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_TEMPORARY = INSTANCE.createKey(
                    "broadcast_temporary",
                    "<red>⚡ <bold><punishment_target></bold> <gray>has been <red><bold>banned</bold></red> <dark_gray>» "
                            + "<yellow><punishment_reason></yellow> <dark_gray>(<gold><punishment_duration></gold>) <dark_gray>by "
                            + "<aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_TEMPORARY_SILENT = INSTANCE.createKey(
                    "broadcast_temporary_silent",
                    "<dark_gray>[<gray>SILENT<dark_gray>]</gray> <red>⚡ <bold><punishment_target></bold> <gray>has been "
                            + "<red><bold>banned</bold></red> <dark_gray>» <yellow><punishment_reason></yellow> <dark_gray>"
                            + "(<gold><punishment_duration></gold>) <dark_gray>by <aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey SUCCESS = INSTANCE.createKey(
                    "success",
                    "<green>✅ Successfully banned <yellow><punishment_target></yellow> for: <#ffa500><punishment_reason></#ffa500>"
            );

            public static final MessageKey SUCCESS_TEMPORARY = INSTANCE.createKey(
                    "success_temporary",
                    "<green>✅ Successfully banned <yellow><punishment_target></yellow> for <gold><punishment_duration></gold>. Reason: <#ffa500><punishment_reason></#ffa500>"
            );
        }

        // Unban related messages
        public static final class Unban extends MessageKeyContainer {

            private static final Unban INSTANCE = new Unban();

            Unban() {
                super("unban", ROOT);
            }

            public final static MessageKey NOT_BANNED = INSTANCE.createKey("not_banned", "<prefix> <dark_red>ERROR: <red>Target '<target>' is not "
                    + "banned or doesn't exist!");

            public final static MessageKey SUCCESS = INSTANCE.createKey("success", "<prefix> <gray>Unbanned player <green> '<target>'");
        }

        // Unban related messages
        public static final class Unmute extends MessageKeyContainer {

            private static final Unmute INSTANCE = new Unmute();

            Unmute() {
                super("unmute", ROOT);
            }

            public final static MessageKey
                    NOT_MUTED = INSTANCE.createKey("not_muted", "<prefix> <dark_red>ERROR: <red>Player '<target>' is not muted or doesn't exist!");

            public final static MessageKey SUCCESS = INSTANCE.createKey("success", "<prefix> <gray>Unmuted player <green> '<target>'");

        }

        // Kick related messages
        public static final class Kick extends MessageKeyContainer {
            private static final Kick INSTANCE = new Kick();

            private Kick() {
                super("kick", ROOT);
            }

            public static final MessageKey MESSAGE = INSTANCE.createKey(
                    "message",
                    "<gradient:#ffaa00:#ff5555><bold>⚠ You were kicked ⚠</bold></gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray>📝 </gray><white>Reason:</white> <#ffa500><punishment_reason></#ffa500>\n" +
                            "<gray>👮 </gray><white>By:</white> <yellow><punishment_issuer></yellow>\n" +
                            "<gray>📅 </gray><white>Issued:</white> <aqua><punishment_issued_date></aqua>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray><italic>You may rejoin unless otherwise restricted.</italic></gray>"
            );

            public static final MessageKey BROADCAST = INSTANCE.createKey(
                    "broadcast",
                    "<red>⚡ <bold><punishment_target></bold> <gray>has been <red><bold>kicked</bold></red> <dark_gray>» "
                            + "<yellow><punishment_reason></yellow> <dark_gray>by <aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_SILENT = INSTANCE.createKey(
                    "broadcast_silent",
                    "<dark_gray>[<gray>SILENT<dark_gray>]</gray> <red>⚡ <bold><punishment_target></bold> <gray>has been "
                            + "<red><bold>kicked</bold></red> <dark_gray>» <yellow><punishment_reason></yellow> <dark_gray>by "
                            + "<aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey SUCCESS = INSTANCE.createKey(
                    "success",
                    "<green>✅ Successfully kicked <yellow><punishment_target></yellow> for: <#ffa500><punishment_reason></#ffa500>"
            );
        }

        // Mute related messages
        public static final class Mute extends MessageKeyContainer {

            private static final Mute INSTANCE = new Mute();

            private Mute() {
                super("mute", ROOT);
            }

            public static final MessageKey ALREADY_MUTED = INSTANCE.createKey("already_muted", "<prefix> <dark_red>ERROR:</dark_red> <red>User "
                    + "<target> "
                    + "is already muted!");


            public static final MessageKey NOTIFICATION_PERMANENT = INSTANCE.createKey(
                    "notification_permanent",
                    "<gradient:#ffaa00:#ff6600><bold>🔇 SERVER MUTE NOTICE 🔇</bold></gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gold><bold>You have been muted on this server!</bold></gold>\n" +
                            "<gray><italic>You cannot send messages in chat</italic></gray>\n" +
                            "<gray>📋 </gray><white>Punishment ID:</white> <yellow>#<punishment_id></yellow>\n" +
                            "<gray>📝 </gray><white>Reason:</white> <#ffa500><punishment_reason></#ffa500>\n" +
                            "<gray>📅 </gray><white>Issued:</white> <aqua><punishment_issued_date></aqua>\n" +
                            "<gray>♾️ </gray><gradient:#ff9900:#cc6600>This mute will never expire!</gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray><italic>Contact staff if you believe this is an error</italic></gray>"
            );

            public static final MessageKey NOTIFICATION_TEMPORARY = INSTANCE.createKey(
                    "notification_temporary",
                    "<gradient:#ffaa00:#ff6600><bold>🔇 SERVER MUTE NOTICE 🔇</bold></gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gold><bold>You have been muted on this server!</bold></gold>\n" +
                            "<gray><italic>You cannot send messages in chat</italic></gray>\n" +
                            "<gray>📋 </gray><white>Punishment ID:</white> <yellow>#<punishment_id></yellow>\n" +
                            "<gray>📝 </gray><white>Reason:</white> <#ffa500><punishment_reason></#ffa500>\n" +
                            "<gray>📅 </gray><white>Issued:</white> <aqua><punishment_issued_date></aqua>\n" +
                            "<gray>⏰ </gray><white>Expires:</white> <green><punishment_expires_date></green>\n" +
                            "<gray>💬 </gray><white>You can speak again in:</white> <yellow><punishment_time_remaining></yellow>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray><italic>Contact staff if you believe this is an error</italic></gray>"
            );

            public static final MessageKey CHAT_BLOCKED_TEMPORARY = INSTANCE.createKey(
                    "chat_blocked_temporary",
                    "<red>🔇 <bold>You are muted!</bold> <gray>Time remaining: <yellow><punishment_time_left></yellow>"
            );

            public static final MessageKey CHAT_BLOCKED_PERMANENT = INSTANCE.createKey(
                    "chat_blocked_permanent",
                    "<red>🔇 <bold>You are permanently muted!</bold> <gray>Contact staff for appeal."
            );

            public static final MessageKey BROADCAST = INSTANCE.createKey(
                    "broadcast",
                    "<red>⚡ <bold><punishment_target></bold> <gray>has been <red><bold>muted</bold></red> <dark_gray>» "
                            + "<yellow><punishment_reason></yellow> <dark_gray>by <aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_SILENT = INSTANCE.createKey(
                    "broadcast_silent",
                    "<dark_gray>[<gray>SILENT<dark_gray>]</gray> <red>⚡ <bold><punishment_target></bold> <gray>has been "
                            + "<red><bold>muted</bold></red> <dark_gray>» <yellow><punishment_reason></yellow> <dark_gray>by "
                            + "<aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_TEMPORARY = INSTANCE.createKey(
                    "broadcast_temporary",
                    "<red>⚡ <bold><punishment_target></bold> <gray>has been <red><bold>muted</bold></red> <dark_gray>» "
                            + "<yellow><punishment_reason></yellow> <dark_gray>(<gold><punishment_duration></gold>) <dark_gray>by "
                            + "<aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_TEMPORARY_SILENT = INSTANCE.createKey(
                    "broadcast_temporary_silent",
                    "<dark_gray>[<gray>SILENT<dark_gray>]</gray> <red>⚡ <bold><punishment_target></bold> <gray>has been "
                            + "<red><bold>muted</bold></red> <dark_gray>» <yellow><punishment_reason></yellow> <dark_gray>"
                            + "(<gold><punishment_duration></gold>) <dark_gray>by <aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey SUCCESS = INSTANCE.createKey(
                    "success",
                    "<green>✅ Successfully muted <yellow><punishment_target></yellow> for: <#ffa500><punishment_reason></#ffa500>"
            );

            public static final MessageKey SUCCESS_TEMPORARY = INSTANCE.createKey(
                    "success_temporary",
                    "<green>✅ Successfully muted <yellow><punishment_target></yellow> for <gold><punishment_duration></gold>. Reason: <#ffa500><punishment_reason></#ffa500>"
            );
        }

        // Warn related messages
        public static final class Warn extends MessageKeyContainer {
            private static final Warn INSTANCE = new Warn();

            private Warn() {
                super("warn", ROOT);
            }

            public static final MessageKey NOTIFICATION = INSTANCE.createKey(
                    "notification",
                    "<gradient:#ffff00:#ff8800><bold>⚠ SERVER WARNING ⚠</bold></gradient>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<yellow><bold>You have received a warning!</bold></yellow>\n" +
                            "<gray><italic>Please follow the server rules</italic></gray>\n" +
                            "<gray>📋 </gray><white>Warning ID:</white> <yellow>#<punishment_id></yellow>\n" +
                            "<gray>📝 </gray><white>Reason:</white> <#ffa500><punishment_reason></#ffa500>\n" +
                            "<gray>📅 </gray><white>Issued:</white> <aqua><punishment_issued_date></aqua>\n" +
                            "<gray>👮 </gray><white>By:</white> <yellow><punishment_issuer></yellow>\n" +
                            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n" +
                            "<gray><italic>Further violations may result in punishment</italic></gray>"
            );

            public static final MessageKey BROADCAST = INSTANCE.createKey(
                    "broadcast",
                    "<red>⚡ <bold><punishment_target></bold> <gray>has been <red><bold>warned</bold></red> <dark_gray>» "
                            + "<yellow><punishment_reason></yellow> <dark_gray>by <aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey BROADCAST_SILENT = INSTANCE.createKey(
                    "broadcast_silent",
                    "<dark_gray>[<gray>SILENT<dark_gray>]</gray> <red>⚡ <bold><punishment_target></bold> <gray>has been "
                            + "<red><bold>warned</bold></red> <dark_gray>» <yellow><punishment_reason></yellow> <dark_gray>by "
                            + "<aqua><punishment_issuer></aqua>"
            );

            public static final MessageKey SUCCESS = INSTANCE.createKey(
                    "success",
                    "<green>✅ Successfully warned <yellow><punishment_target></yellow> for: <#ffa500><punishment_reason></#ffa500>"
            );
        }
    }

    // Command system messages
    public static final class Commands extends MessageKeyContainer {
        private static final Commands ROOT = new Commands();

        private Commands() {
            super("commands", null);
        }

        public static final MessageKey NO_PERMISSION = ROOT.createKey(
                "no_permission",
                "<red>You don't have permission to execute this command!"
        );

        public static final MessageKey INVALID_USAGE = ROOT.createKey(
                "invalid_usage",
                "<red>Invalid usage! Use: <usage>"
        );

        public static final MessageKey PLAYER_NOT_FOUND = ROOT.createKey(
                "player_not_found",
                "<red>Player '<player>' not found!"
        );

        public static final MessageKey CONSOLE_ONLY = ROOT.createKey(
                "console_only",
                "<red>This command can only be executed from console!"
        );

        public static final MessageKey PLAYER_ONLY = ROOT.createKey(
                "player_only",
                "<red>This command can only be executed by players!"
        );
    }

    // Economy system messages
    public static final class Economy extends MessageKeyContainer {
        private static final Economy ROOT = new Economy();

        private Economy() {
            super("economy", null);
        }

        public static final MessageKey BALANCE = ROOT.createKey(
                "balance",
                "<green>Your balance: <gold><amount> coins"
        );

        public static final MessageKey INSUFFICIENT_FUNDS = ROOT.createKey(
                "insufficient_funds",
                "<red>You don't have enough coins! Required: <gold><amount>"
        );

        public static final MessageKey PAYMENT_SENT = ROOT.createKey(
                "payment_sent",
                "<green>Sent <gold><amount> coins <green>to <player>"
        );

        public static final MessageKey PAYMENT_RECEIVED = ROOT.createKey(
                "payment_received",
                "<green>Received <gold><amount> coins <green>from <player>"
        );
    }
}