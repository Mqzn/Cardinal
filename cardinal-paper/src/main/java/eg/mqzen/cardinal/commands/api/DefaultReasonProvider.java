package eg.mqzen.cardinal.commands.api;

import eg.mqzen.lib.commands.command.parameters.CommandParameter;
import eg.mqzen.lib.commands.command.parameters.OptionalValueSupplier;
import eg.mqzen.lib.commands.context.ExecutionContext;
import eg.mqzen.lib.commands.context.Source;
import eg.mqzen.lib.config.YamlDocument;
import eg.mqzen.cardinal.Cardinal;
import org.jetbrains.annotations.Nullable;

public final class DefaultReasonProvider implements OptionalValueSupplier {

    @Nullable
    @Override
    public <S extends Source> String supply(ExecutionContext<S> context, CommandParameter<S> parameter) {
        YamlDocument configYaml = Cardinal.getInstance().getConfigYaml();
        if(configYaml == null) {
            return "Breaking Server Rules";
        }
        return configYaml.getString("default-reason");
    }
}
