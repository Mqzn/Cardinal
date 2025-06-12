package net.mineabyss.core.commands.api.exceptions;

import com.mineabyss.lib.commands.ImperatConfig;
import com.mineabyss.lib.commands.context.Context;
import com.mineabyss.lib.commands.context.Source;
import com.mineabyss.lib.commands.exception.ImperatException;
import com.mineabyss.lib.commands.exception.SelfHandledException;
import net.mineabyss.core.commands.api.CardinalSource;

public final class CardinalSourceException extends SelfHandledException {

    private final String msg;

    public CardinalSourceException(String msg, Object... args) {
        this.msg = String.format(msg, args);
    }


    @Override
    public <S extends Source> void handle(ImperatConfig<S> imperat, Context<S> context) {
        var sourceResolver = imperat.getSourceResolver(context.source().getClass());
        if(sourceResolver != null) {
            try {
                CardinalSource cardinalSource = (CardinalSource) sourceResolver.resolve(context.source());
                cardinalSource.sendMsg(msg);
            } catch (ImperatException e) {
                e.printStackTrace();
            }
        }
    }
}
