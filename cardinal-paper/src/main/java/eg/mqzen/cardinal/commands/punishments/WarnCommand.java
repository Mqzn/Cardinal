package eg.mqzen.cardinal.commands.punishments;

import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.util.Pair;
import eg.mqzen.lib.commands.BukkitSource;
import eg.mqzen.lib.commands.annotations.Command;
import eg.mqzen.lib.commands.annotations.ContextResolved;
import eg.mqzen.lib.commands.annotations.Dependency;
import eg.mqzen.lib.commands.annotations.Greedy;
import eg.mqzen.lib.commands.annotations.Named;
import eg.mqzen.lib.commands.annotations.Optional;
import eg.mqzen.lib.commands.annotations.Switch;
import eg.mqzen.lib.commands.annotations.Usage;
import eg.mqzen.lib.commands.context.ExecutionContext;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.config.MessageKey;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.config.MessageKeys;
import eg.mqzen.cardinal.util.PunishmentMessageUtil;

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
