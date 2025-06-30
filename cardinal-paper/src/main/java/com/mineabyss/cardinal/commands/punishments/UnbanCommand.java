package com.mineabyss.cardinal.commands.punishments;

import com.mineabyss.cardinal.util.Pair;
import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Default;
import com.mineabyss.lib.commands.annotations.Dependency;
import com.mineabyss.lib.commands.annotations.Description;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Permission;
import com.mineabyss.lib.commands.annotations.Usage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import com.mineabyss.cardinal.api.config.MessageConfig;
import com.mineabyss.cardinal.api.punishments.Punishable;
import com.mineabyss.cardinal.api.punishments.Punishment;
import com.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import com.mineabyss.cardinal.api.punishments.StandardPunishmentType;
import com.mineabyss.cardinal.Cardinal;
import com.mineabyss.cardinal.CardinalPermissions;
import com.mineabyss.cardinal.commands.api.AllowsPunishmentID;
import com.mineabyss.cardinal.commands.api.CardinalSource;
import com.mineabyss.cardinal.config.MessageKeys;

import java.util.concurrent.CompletableFuture;

@Command("unban")
@Permission(CardinalPermissions.UNBAN_COMMAND_PERMISSION)
@Description("Unbans a player from the server.")
public class UnbanCommand {

    @Dependency
    private MessageConfig config;

    @Usage
    public void def(CardinalSource source) {
        // /unban
        source.sendMsg("<red>/Unban <user> [reason]");
    }

    @Usage
    public void exec(
            PunishmentIssuer issuer,
            @Named("user") @AllowsPunishmentID CompletableFuture<Punishable<?>> targetFuture,
            @Default("Appealed") @Greedy @Named("reason") String reason) {

        targetFuture.thenApplyAsync((target)-> {
            java.util.Optional<Punishment<?>> punishmentContainer =  target.fetchPunishment(StandardPunishmentType.BAN).unwrap().join();
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
                issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Unban.SUCCESS, Placeholder.unparsed("target", target.getTargetName())));
            }
            else {
                issuer.sendMsg(config.getMessage(MessageKeys.Punishments.Unban.NOT_BANNED, Placeholder.unparsed("target", target.getTargetName())));
            }
        });


    }


}
