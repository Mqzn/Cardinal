package net.mineabyss.cardinal.api;

import net.mineabyss.cardinal.api.punishments.PunishmentManager;
import org.jetbrains.annotations.NotNull;

public interface CardinalAPI {

    @NotNull PunishmentManager getPunishmentManager();

}
