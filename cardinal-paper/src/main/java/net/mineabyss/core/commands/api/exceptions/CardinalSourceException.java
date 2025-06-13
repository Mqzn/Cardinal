package net.mineabyss.core.commands.api.exceptions;

import com.mineabyss.lib.commands.exception.ImperatException;
import lombok.Getter;

public final class CardinalSourceException extends ImperatException {

    @Getter
    private final String msg;

    public CardinalSourceException(String msg, Object... args) {
        this.msg = String.format(msg, args);
    }

}
