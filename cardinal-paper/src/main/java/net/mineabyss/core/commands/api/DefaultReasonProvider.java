package net.mineabyss.core.commands.api;

import com.mineabyss.lib.commands.command.parameters.CommandParameter;
import com.mineabyss.lib.commands.command.parameters.OptionalValueSupplier;
import com.mineabyss.lib.commands.context.Source;
import com.mineabyss.lib.config.YamlDocument;
import net.mineabyss.core.Cardinal;
import org.jetbrains.annotations.Nullable;

public final class DefaultReasonProvider implements OptionalValueSupplier {

    @Nullable
    @Override
    public <S extends Source> String supply(S source, CommandParameter<S> parameter) {
        YamlDocument configYaml = Cardinal.getInstance().getConfigYaml();
        if(configYaml == null) {
            return "Breaking Server Rules";
        }
        return configYaml.getString("default-reason");
    }
}
