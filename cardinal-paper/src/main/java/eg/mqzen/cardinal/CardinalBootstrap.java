package eg.mqzen.cardinal;

import eg.mqzen.lib.bootstrap.MPluginBoot;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("all")
public class CardinalBootstrap implements PluginBootstrap {

    private final MPluginBoot cardinalBoot;

    public CardinalBootstrap() {
        this.cardinalBoot = new MPluginBoot((pluginProviderContext) -> {
            //create our imperat
            return new Cardinal();
        });
    }

    @Override
    public void bootstrap(BootstrapContext bootstrapContext) {
        cardinalBoot.bootstrap(bootstrapContext);
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return cardinalBoot.createPlugin(context);
    }
}
