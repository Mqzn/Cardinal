package net.mineabyss.core.util;

import com.mineabyss.lib.util.TimeUtil;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mineabyss.cardinal.api.punishments.Punishment;
import net.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class PunishmentMessageUtil {

    public static @NotNull String getKickMessageMiniMessage(Punishment<?> punishment) {
        if (punishment.getType() == StandardPunishmentType.WARN || punishment.getType() == StandardPunishmentType.MUTE) {
            return "";
        }

        StringBuilder kickMsg = new StringBuilder();

        // Header with gradient
        kickMsg.append("<gradient:#ff4444:#cc0000><bold>⚠ SERVER BAN NOTICE ⚠</bold></gradient>\n");
        kickMsg.append("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n\n");

        // Main message
        kickMsg.append("<red><bold>You have been banned from this server!</bold></red>\n\n");

        // Punishment details with professional styling
        kickMsg.append("<gray>📋 </gray><white>Punishment ID:</white> <yellow>#")
                .append(punishment.getId().getRepresentation())
                .append("</yellow>\n");

        kickMsg.append("<gray>📝 </gray><white>Reason:</white> <#ffa500>")
                .append(punishment.getReason().orElse("Breaking the server rules"))
                .append("</#ffa500>\n");

        kickMsg.append("<gray>📅 </gray><white>Issued:</white> <aqua>")
                .append(TimeUtil.formatDate(punishment.getIssuedAt()))
                .append("</aqua>\n");

        // Expiry information
        if (!punishment.isPermanent()) {
            kickMsg.append("<gray>⏰ </gray><white>Expires:</white> <green>")
                    .append(TimeUtil.formatDate(punishment.getExpiresAt()))
                    .append("</green>\n");
        } else {
            kickMsg.append("<gray>♾️ </gray><gradient:#ff6b6b:#cc0000>This ban will never expire!</gradient>\n");
        }

        // Footer
        kickMsg.append("\n<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n");
        kickMsg.append("<gray><italic>Contact staff if you believe this is an error</italic></gray>");

        return kickMsg.toString();
    }

    public static @NotNull Component getKickMessage(Punishment<?> punishment) {
        String miniMessage = getKickMessageMiniMessage(punishment);
        if (miniMessage.isEmpty()) {
            return Component.empty();
        }
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

}
