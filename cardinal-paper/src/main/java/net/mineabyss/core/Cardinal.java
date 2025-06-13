package net.mineabyss.core;

import com.mineabyss.lib.ConfigLoader;
import com.mineabyss.lib.Events;
import com.mineabyss.lib.bootstrap.MineAbyssPlugin;
import com.mineabyss.lib.commands.BukkitImperat;
import lombok.Getter;
import net.mineabyss.cardinal.api.CardinalAPI;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import net.mineabyss.cardinal.api.punishments.PunishmentManager;
import net.mineabyss.cardinal.api.storage.StorageException;
import net.mineabyss.core.commands.punishments.UnMuteCommand;
import net.mineabyss.core.commands.api.CardinalSource;
import net.mineabyss.core.commands.api.DurationParameterType;
import net.mineabyss.core.commands.api.exceptions.CardinalSourceException;
import net.mineabyss.core.commands.punishments.BanCommand;
import net.mineabyss.core.commands.punishments.MuteCommand;
import net.mineabyss.core.commands.punishments.UnbanCommand;
import net.mineabyss.core.listener.BanListener;
import net.mineabyss.core.listener.MuteListener;
import net.mineabyss.core.punishments.StandardPunishmentManager;
import net.mineabyss.core.punishments.issuer.PunishmentIssuerFactory;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class Cardinal extends MineAbyssPlugin implements CardinalAPI {

    @Getter private static Cardinal instance;

    private PunishmentManager punishmentManager;

    public Cardinal(
    ) {
        super(null);
    }


    @Override
    public BukkitImperat loadImperat() {
        return BukkitImperat.builder(this)
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
                .build();
    }

    @Override
    protected void registerPluginCommands(@NotNull BukkitImperat bukkitImperat) {

        bukkitImperat.registerCommands(
                new BanCommand(),
                new UnbanCommand(),
                new MuteCommand(),
                new UnMuteCommand()
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

        this.configLoader = ConfigLoader.builder("config.yml")
                .parentDirectory(getDataFolder())
                .copyDefaults(true)
                .build();
        loadConfiguration();
    }

    @Override
    protected void onStart() {
        //TODO start core
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
}
