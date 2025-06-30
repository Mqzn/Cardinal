package com.mineabyss.cardinal.commands.punishments;

import com.mineabyss.cardinal.api.punishments.Punishable;
import com.mineabyss.cardinal.commands.api.AllowsPunishmentID;
import com.mineabyss.cardinal.util.Pair;
import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Dependency;
import com.mineabyss.lib.commands.annotations.Description;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Optional;
import com.mineabyss.lib.commands.annotations.Permission;
import com.mineabyss.lib.commands.annotations.Usage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import com.mineabyss.cardinal.api.config.MessageConfig;
import com.mineabyss.cardinal.api.punishments.Punishment;
import com.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import com.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import com.mineabyss.cardinal.Cardinal;
import com.mineabyss.cardinal.CardinalPermissions;
import com.mineabyss.cardinal.commands.api.CardinalSource;
import com.mineabyss.cardinal.config.MessageKeys;

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
