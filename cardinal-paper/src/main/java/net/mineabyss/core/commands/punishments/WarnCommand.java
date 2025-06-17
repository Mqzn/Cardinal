package net.mineabyss.core.commands.punishments;

import com.mineabyss.lib.commands.BukkitSource;
import com.mineabyss.lib.commands.annotations.Command;
import com.mineabyss.lib.commands.annotations.Greedy;
import com.mineabyss.lib.commands.annotations.Named;
import com.mineabyss.lib.commands.annotations.Usage;
import com.mineabyss.lib.commands.context.ExecutionContext;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.core.commands.api.CardinalSource;

@Command("warn")
public class WarnCommand {

    @Usage
    public void def(CardinalSource source, ExecutionContext<BukkitSource> context) {
        source.sendMsg("<red>/" + context.label() + " " + context.getDetectedUsage().formatted());
    }

    @Usage
    public void exec(CardinalSource source, @Named("user")Punishable<?> punishable, @Greedy String reason) {

    }

}
