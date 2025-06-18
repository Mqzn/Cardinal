package net.mineabyss.core;

import com.mineabyss.lib.ConfigLoader;
import com.mineabyss.lib.Events;
import com.mineabyss.lib.bootstrap.MineAbyssPlugin;
import com.mineabyss.lib.commands.BukkitImperat;
import com.mineabyss.lib.commands.util.TypeWrap;
import com.mineabyss.lib.gui.Lotus;
import lombok.Getter;
import net.mineabyss.cardinal.api.CardinalAPI;
import net.mineabyss.cardinal.api.CardinalProvider;
import net.mineabyss.cardinal.api.config.MessageConfig;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.PunishmentManager;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.core.commands.api.PunishableParameterType;
import net.mineabyss.core.commands.punishments.HistoryCommand;
import net.mineabyss.core.commands.punishments.KickCommand;
import net.mineabyss.core.commands.punishments.UnMuteCommand;
import net.mineabyss.core.commands.api.CardinalSource;
import net.mineabyss.core.commands.api.DurationParameterType;
import net.mineabyss.core.commands.api.exceptions.CardinalSourceException;
import net.mineabyss.core.commands.punishments.BanCommand;
import net.mineabyss.core.commands.punishments.MuteCommand;
import net.mineabyss.core.commands.punishments.UnbanCommand;
import net.mineabyss.core.commands.punishments.WarnCommand;
import net.mineabyss.core.config.YamlMessageConfig;
import net.mineabyss.core.listener.BanListener;
import net.mineabyss.core.listener.MuteListener;
import net.mineabyss.core.punishments.StandardPunishmentManager;
import net.mineabyss.core.punishments.issuer.PunishmentIssuerFactory;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class Cardinal extends MineAbyssPlugin implements CardinalAPI {

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
                .sourceResolver(CardinalSource.class, CardinalSource::new)
                .sourceResolver(PunishmentIssuer.class,(source -> {
                    if(source.isConsole()) {
                        return PunishmentIssuerFactory.fromConsole();
                    }
                    return PunishmentIssuerFactory.fromPlayer(source.asPlayer());
                }))
                .throwableResolver(CardinalSourceException.class, (ex, imperat, context)-> {
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
        //TODO start core
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
