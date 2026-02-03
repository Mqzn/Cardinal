package eg.mqzen.cardinal.util;

import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.CardinalPermissions;
import eg.mqzen.cardinal.api.config.MessageKey;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.config.MessageKeys;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class PunishmentMessageUtil {

    public static void broadcastPunishment(final MessageKey key, MessageKey silentKey, final Punishment<?> punishment, final boolean silent) {

        final Component bcMsg = Cardinal.getInstance().getMessagesConfig().getMessage(silent ? silentKey : key, punishment.asTagResolver());
        if(silent) {
            punishment.getIssuer().sendMsg(bcMsg);
        }else {
            Bukkit.broadcast(
                    Cardinal.getInstance().getMessagesConfig().getMessage(key, punishment.asTagResolver()),
                    CardinalPermissions.STAFF_NOTIFY
            );
        }
    }

    public static Component getBanKickMessage(Punishment<?> punishment) {

        MessageKey key = punishment.isPermanent() ? MessageKeys.Punishments.Ban.KICK_MESSAGE_PERMANENT :
                MessageKeys.Punishments.Ban.KICK_MESSAGE_TEMPORARY;
        return Cardinal.getInstance().getMessagesConfig().getMessage(key, punishment.asTagResolver());
    }

    public static @NotNull Component getMuteChatBlock(Punishment<?> punishment) {
        MessageKey key = punishment.isPermanent() ? MessageKeys.Punishments.Mute.CHAT_BLOCKED_PERMANENT :
                MessageKeys.Punishments.Mute.CHAT_BLOCKED_TEMPORARY;
        return Cardinal.getInstance().getMessagesConfig().getMessage(key, punishment.asTagResolver());
    }
}
