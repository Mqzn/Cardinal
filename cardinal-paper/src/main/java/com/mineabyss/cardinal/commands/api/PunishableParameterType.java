package com.mineabyss.cardinal.commands.api;

import com.mineabyss.lib.commands.BukkitSource;
import com.mineabyss.lib.commands.command.parameters.CommandParameter;
import com.mineabyss.lib.commands.command.parameters.type.BaseParameterType;
import com.mineabyss.lib.commands.context.ExecutionContext;
import com.mineabyss.lib.commands.context.internal.CommandInputStream;
import com.mineabyss.lib.commands.exception.ImperatException;
import com.mineabyss.cardinal.api.punishments.Punishable;
import com.mineabyss.cardinal.commands.api.exceptions.CardinalSourceException;
import com.mineabyss.cardinal.util.PunishmentIDGenerator;
import com.mineabyss.cardinal.punishments.target.PunishmentTargetFactory;
import com.mineabyss.cardinal.util.IPUtils;
import com.mineabyss.cardinal.util.TypeUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public final class PunishableParameterType extends BaseParameterType<BukkitSource, Punishable<?>> {

    public PunishableParameterType() {
        super();
    }

    @NotNull @Override
    public Punishable<?> resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> inputStream,
            @NotNull String input
    ) throws ImperatException {

        if(IPUtils.isValidIP(input)) {
            return PunishmentTargetFactory.ipTarget(input);
        }else if(TypeUtils.isUUID(input)) {
            context.source().reply(
                    Component.text("Attempting to fetch the player profile, this may take few seconds...", NamedTextColor.GREEN)
                            .decorate(TextDecoration.ITALIC)
            );
            OfflinePlayer playerByUUID = Bukkit.getOfflinePlayer(UUID.fromString(input));
            return PunishmentTargetFactory.playerTarget(playerByUUID);
        }else {

            CommandParameter<?> parameter = inputStream.currentParameter().orElseThrow();
            if(parameter.isAnnotated()
                    && parameter.asAnnotated().hasAnnotation(AllowsPunishmentID.class)

            ) {
                boolean isValidID = PunishmentIDGenerator.isValidPunishmentID(input);
                System.out.println("Punishment ID: " + input + " is valid: " + isValidID);

                if(isValidID) {
                    return PunishmentTargetFactory.punishmentID(input.substring(1));
                }
            }else {
                System.out.println("Punishment ID annotation not present or invalid punishment-id for input: " + input);
            }

            context.source().reply(
                    Component.text("Attempting to fetch the player profile, this may take few seconds...", NamedTextColor.GREEN)
                            .decorate(TextDecoration.ITALIC)
            );
            
            OfflinePlayer player = Bukkit.getOfflinePlayer(input);
            if(player.getName() == null) {
                throw new CardinalSourceException("Player with name '" + input + "' does not exist !");
            }
            return PunishmentTargetFactory.playerTarget(player);
        }
    }
}
