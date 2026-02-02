package eg.mqzen.cardinal.commands.api.exceptions;

import eg.mqzen.lib.commands.context.Context;
import eg.mqzen.lib.commands.exception.ImperatException;
import lombok.Getter;

public final class CardinalSourceException extends ImperatException {

    @Getter
    private final String msg;

    public  CardinalSourceException(String msg, Context<?> ctx, Object... args) {
        super(ctx);
        this.msg = String.format(msg, args);
    }

}
