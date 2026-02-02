package eg.mqzen.cardinal.commands.punishments;

import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.commands.api.AllowsPunishmentID;
import eg.mqzen.cardinal.util.Pair;
import eg.mqzen.lib.commands.annotations.Command;
import eg.mqzen.lib.commands.annotations.Dependency;
import eg.mqzen.lib.commands.annotations.Description;
import eg.mqzen.lib.commands.annotations.Greedy;
import eg.mqzen.lib.commands.annotations.Named;
import eg.mqzen.lib.commands.annotations.Optional;
import eg.mqzen.lib.commands.annotations.Permission;
import eg.mqzen.lib.commands.annotations.Usage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import eg.mqzen.cardinal.api.config.MessageConfig;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.CardinalPermissions;
import eg.mqzen.cardinal.commands.api.CardinalSource;
import eg.mqzen.cardinal.config.MessageKeys;

import java.util.concurrent.CompletableFuture;

@Command("unmute")
@Permission(CardinalPermissions.UNMUTE_COMMAND_PERMISSION)
@Description("Unmutes a player on the server.")
public final class UnMuteCommand {

    @Dependency
    private MessageConfig config;

    @Usage
    public void def(CardinalSource source) {
        source.sendMsg("<red>Unmute <target> [reason...]");
    }

    @Usage
    public void unmute(
            PunishmentIssuer issuer,
            @Named("target") @AllowsPunishmentID CompletableFuture<Punishable<?>> targetFuture,
            @Named("reason") @Greedy @Optional String reason
    ) {
        targetFuture.thenApplyAsync((target)-> {
            java.util.Optional<Punishment<?>> punishmentContainer =  target.fetchPunishment(StandardPunishmentType.MUTE).unwrap().join();
            return new Pair<>(target, punishmentContainer);
        }).thenApplyAsync((data)-> {

            var punishmentContainer = data.right();
            Punishable<?> target = data.left();

            if(punishmentContainer.isEmpty()) {
                return new Pair<>(false, target);
            }else {

                Punishment<?> punishment = punishmentContainer.get();
                return Cardinal.getInstance().getPunishmentManager()
                        .revokePunishment(punishment.getId(), issuer, reason)
                        .map((revoked)-> new Pair<>(revoked, target))
                        .unwrap()
                        .join();
            }

        }).whenComplete((data, ex)-> {
            if(ex != null) {ex.printStackTrace();}

            var revoked = data.left();
            var target = data.right();
            if(revoked) {
                issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Unmute.SUCCESS, Placeholder.unparsed("target", target.getTargetName())));
            }
            else {
                issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Unmute.NOT_MUTED, Placeholder.unparsed("target", target.getTargetName())));
            }
        });
    }



}
