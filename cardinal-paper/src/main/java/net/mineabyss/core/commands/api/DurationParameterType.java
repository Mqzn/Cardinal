package net.mineabyss.core.commands.api;

import com.mineabyss.lib.commands.BukkitSource;
import com.mineabyss.lib.commands.command.parameters.OptionalValueSupplier;
import com.mineabyss.lib.commands.command.parameters.type.BaseParameterType;
import com.mineabyss.lib.commands.context.ExecutionContext;
import com.mineabyss.lib.commands.context.internal.CommandInputStream;
import com.mineabyss.lib.commands.exception.ImperatException;
import com.mineabyss.lib.util.TimeUtil;
import net.mineabyss.core.commands.api.exceptions.CardinalSourceException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;

public class DurationParameterType extends BaseParameterType<BukkitSource, Duration> {

    public DurationParameterType() {
        super(Duration.class);
    }

    @Nullable @Override
    public Duration resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> inputStream,
            String input
    ) throws ImperatException {
        try{
            return TimeUtil.parse(input);
        }catch (Exception exception) {
            throw new CardinalSourceException("<red>Invalid duration '%s'", input);
        }
    }

    @Override
    public OptionalValueSupplier supplyDefaultValue() {
        return OptionalValueSupplier.of("permanent");
    }
}
