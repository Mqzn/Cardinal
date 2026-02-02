package eg.mqzen.cardinal.api;

import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.punishments.PunishmentManager;
import org.jetbrains.annotations.NotNull;

public interface CardinalAPI {

    @NotNull PunishmentManager getPunishmentManager();

    @NotNull MessageConfig getMessagesConfig();

}
