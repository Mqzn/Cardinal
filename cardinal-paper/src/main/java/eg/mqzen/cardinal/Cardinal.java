package eg.mqzen.cardinal;

import eg.mqzen.lib.ConfigLoader;
import eg.mqzen.lib.Events;
import eg.mqzen.lib.bootstrap.MPlugin;
import eg.mqzen.lib.commands.BukkitImperat;
import eg.mqzen.lib.commands.BukkitSource;
import eg.mqzen.lib.commands.context.ExecutionContext;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.lib.gui.Lotus;
import lombok.Getter;
import eg.mqzen.cardinal.api.CardinalAPI;
import eg.mqzen.cardinal.api.CardinalProvider;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.PunishmentManager;
import eg.mqzen.cardinal.api.storage.StorageException;
import eg.mqzen.cardinal.commands.api.PunishableParameterType;
import eg.mqzen.cardinal.commands.punishments.HistoryCommand;
import eg.mqzen.cardinal.commands.punishments.KickCommand;
import eg.mqzen.cardinal.commands.punishments.UnMuteCommand;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.commands.api.DurationParameterType;
import eg.mqzen.cardinal.commands.api.exceptions.CardinalSourceException;
import eg.mqzen.cardinal.commands.punishments.BanCommand;
import eg.mqzen.cardinal.commands.punishments.MuteCommand;
import eg.mqzen.cardinal.commands.punishments.UnbanCommand;
import eg.mqzen.cardinal.commands.punishments.WarnCommand;
import eg.mqzen.cardinal.config.YamlMessageConfig;
import eg.mqzen.cardinal.listener.BanListener;
import eg.mqzen.cardinal.listener.MuteListener;
import eg.mqzen.cardinal.punishments.StandardPunishmentManager;
import eg.mqzen.cardinal.punishments.issuer.PunishmentIssuerFactory;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class Cardinal extends MPlugin implements CardinalAPI {

    @Getter private static Cardinal instance;

    private MessageConfig config;
    private PunishmentManager punishmentManager;

    @Getter private Lotus lotus;

    public Cardinal(
    ) {
        super(null);
    }


    @Override
    public BukkitImperat loadImperat() {
        return BukkitImperat.builder(this)
                .dependencyResolver(MessageConfig.class, ()-> config)
                .contextResolver(new TypeWrap<ExecutionContext<BukkitSource>>(){}.getType(), (ctx, param)-> ctx)
                .sourceResolver(CardinalSource.class, (src, ctx)-> new CardinalSource(src))
                .sourceResolver(PunishmentIssuer.class,(source, ctx) -> {
                    if(source.isConsole()) {
                        return PunishmentIssuerFactory.fromConsole();
                    }
                    return PunishmentIssuerFactory.fromPlayer(source.asPlayer());
                })
                .throwableResolver(CardinalSourceException.class, (ex, context)-> {
                    context.source().origin().sendRichMessage(ex.getMsg());
                })
                .parameterType(Duration.class, new DurationParameterType())
                .parameterType(new TypeWrap<Punishable<?>>(){}.getType(), new PunishableParameterType())
                .build();
    }

    @Override
    protected void registerPluginCommands(@NotNull BukkitImperat bukkitImperat) {
        bukkitImperat.registerCommands(
                new KickCommand(),
                new BanCommand(),
                new UnbanCommand(),
                new MuteCommand(),
                new UnMuteCommand(),
                new WarnCommand(),
                new HistoryCommand()
        );
    }

    @Override
    protected void registerPluginListeners() {
        Events.listen(this,
                new BanListener(),
                new MuteListener()
        );
    }

    @Override
    protected void onPreStart() {
        instance = this;
        CardinalProvider.load(instance);

        this.configLoader = ConfigLoader.builder("config.yml")
                .parentDirectory(getDataFolder())
                .copyDefaults(true)
                .build();
        this.loadConfiguration();

        config = new YamlMessageConfig(
                loadConfiguration(ConfigLoader.builder("language_en.yml")
                .parentDirectory(getDataFolder())
                .copyDefaults(true)
                .build())
        );
    }

    @Override
    protected void onStart() {
        this.lotus = Lotus.load(this);
        try {
            punishmentManager = StandardPunishmentManager.createNew(this.configYaml);
        } catch (StorageException e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }

    }

    @Override
    protected void onStop() {
        //TODO stop core
    }

    public static void log(String msg, Object... args) {
        instance.getLogger().info(String.format(msg, args));
    }

    public static void warn(String msg, Object... args) {
        instance.getLogger().warning(String.format(msg, args));
    }

    public static void severe(String msg, Object... args) {
        instance.getLogger().severe(String.format(msg, args));
    }

    @NotNull
    @Override
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    @NotNull
    @Override
    public MessageConfig getMessagesConfig() {
        return config;
    }

}
