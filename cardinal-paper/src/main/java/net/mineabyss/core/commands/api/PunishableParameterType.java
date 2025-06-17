package net.mineabyss.core.commands.api;

import com.mineabyss.lib.commands.BukkitSource;
import com.mineabyss.lib.commands.command.parameters.CommandParameter;
import com.mineabyss.lib.commands.command.parameters.type.BaseParameterType;
import com.mineabyss.lib.commands.context.ExecutionContext;
import com.mineabyss.lib.commands.context.internal.CommandInputStream;
import com.mineabyss.lib.commands.exception.ImperatException;
import com.mineabyss.lib.commands.util.TypeWrap;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.core.commands.api.exceptions.CardinalSourceException;
import net.mineabyss.core.punishments.core.PunishmentIDGenerator;
import net.mineabyss.core.punishments.target.PunishmentTargetFactory;
import net.mineabyss.core.util.IPUtils;
import net.mineabyss.core.util.TypeUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public final class PunishableParameterType extends BaseParameterType<BukkitSource, Punishable<?>> {

    public PunishableParameterType() {
        super(new TypeWrap<>() {});
    }

    @NotNull @Override
    public Punishable<?> resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> inputStream,
            String input
    ) throws ImperatException {

        if(IPUtils.isValidIP(input)) {
            return PunishmentTargetFactory.ipTarget(input);
        }else if(TypeUtils.isUUID(input)) {
            OfflinePlayer playerByUUID = Bukkit.getOfflinePlayer(UUID.fromString(input));
            return PunishmentTargetFactory.playerTarget(playerByUUID);
        }else {

            CommandParameter<?> parameter = inputStream.currentParameter().orElseThrow();
            if(parameter.isAnnotated()
                    && parameter.asAnnotated().hasAnnotation(AllowsPunishmentID.class)
                    && PunishmentIDGenerator.isValidPunishmentID(input)
            ) {
                return PunishmentTargetFactory.punishmentID(input);
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(input);
            if(player.getName() == null) {
                throw new CardinalSourceException("Player with name '" + input + "' does not exist !");
            }
            return PunishmentTargetFactory.playerTarget(player);
        }
    }
}
