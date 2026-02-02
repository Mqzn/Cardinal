package eg.mqzen.cardinal.commands.api;

import eg.mqzen.lib.commands.BukkitSource;
import eg.mqzen.lib.commands.command.parameters.OptionalValueSupplier;
import eg.mqzen.lib.commands.command.parameters.type.BaseParameterType;
import eg.mqzen.lib.commands.context.ExecutionContext;
import eg.mqzen.lib.commands.context.internal.CommandInputStream;
import eg.mqzen.lib.commands.exception.ImperatException;
import eg.mqzen.lib.util.TimeUtil;
import eg.mqzen.cardinal.commands.api.exceptions.CardinalSourceException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;

public class DurationParameterType extends BaseParameterType<BukkitSource, Duration> {

    public DurationParameterType() {
        super();
    }

    @Nullable @Override
    public Duration resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
        try{
            return TimeUtil.parse(input);
        }catch (Exception exception) {
            throw new CardinalSourceException("<red>Invalid duration '%s'", context, input);
        }
    }

    @Override
    public OptionalValueSupplier supplyDefaultValue() {
        return OptionalValueSupplier.of("permanent");
    }
}
