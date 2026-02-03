package eg.mqzen.cardinal.commands.punishments;

import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.CardinalPermissions;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.config.MessageKey;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.config.MessageKeys;
import eg.mqzen.cardinal.util.Pair;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.annotations.Dependency;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.context.ExecutionContext;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Command("warn")
public class WarnCommand {

    @Dependency
    private MessageConfig config;

    @Usage
    public void def(CardinalSource source, @ContextResolved ExecutionContext<BukkitSource> context) {
        source.sendMsg("<red>/" + context.label() + " " + context.getDetectedUsage().formatted());
    }

    @Usage
    public void exec(
            PunishmentIssuer issuer,
            @Named("user") CompletableFuture<Punishable<?>> targetFuture,
            @Switch({"silent", "s"}) boolean silent,
            @Optional @Greedy String reason
    ) {
        if(!issuer.hasPermission(CardinalPermissions.USE_SILENT_FLAG_PERMISSION)) {
            issuer.sendMsg("<red>You do not have permission to use the silent flag!");
            return;
        }

        String warnReason;
        if (reason == null) {
            assert Cardinal.getInstance().getConfigYaml() != null;
            warnReason = Cardinal.getInstance().getConfigYaml().getString("default-reason");
        } else {
            warnReason = reason;
        }

        targetFuture.thenApplyAsync((target)-> {

            Punishment<?> punishment = Cardinal.getInstance().getPunishmentManager()
                    .applyPunishment(StandardPunishmentType.WARN, issuer, target, Duration.ZERO, warnReason).join();

            return new Pair<>(target, punishment);
        })
        .whenComplete((data, ex)-> {
            if(ex != null) {ex.printStackTrace();}
            var target = data.left();
            var punishment = data.right();
            TagResolver resolver = punishment.asTagResolver();
            //we notify the victim
            target.sendMsg(config.getMessage(MessageKeys.Punishments.Warn.NOTIFICATION, resolver));

            //we broadcast the message
            MessageKey normalKey = MessageKeys.Punishments.Warn.BROADCAST;
            MessageKey silentKey = MessageKeys.Punishments.Warn.BROADCAST_SILENT;
            PunishmentMessageUtil.broadcastPunishment(normalKey, silentKey, punishment, silent);

            // success
            issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Warn.SUCCESS, resolver));
        });



    }

}
