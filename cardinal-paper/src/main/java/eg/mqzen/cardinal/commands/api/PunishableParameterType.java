package eg.mqzen.cardinal.commands.api;

import eg.mqzen.lib.commands.BukkitSource;
import eg.mqzen.lib.commands.command.parameters.CommandParameter;
import eg.mqzen.lib.commands.command.parameters.type.BaseParameterType;
import eg.mqzen.lib.commands.context.ExecutionContext;
import eg.mqzen.lib.commands.context.internal.CommandInputStream;
import eg.mqzen.lib.commands.exception.ImperatException;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.commands.api.exceptions.CardinalSourceException;
import eg.mqzen.cardinal.util.PunishmentIDGenerator;
import eg.mqzen.cardinal.punishments.target.PunishmentTargetFactory;
import eg.mqzen.cardinal.util.IPUtils;
import eg.mqzen.cardinal.util.TypeUtils;
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
                throw new CardinalSourceException("Player with name '" + input + "' does not exist !", context);
            }
            return PunishmentTargetFactory.playerTarget(player);
        }
    }
}
