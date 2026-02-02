package eg.mqzen.cardinal.commands.api;

import eg.mqzen.lib.commands.BukkitSource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CardinalSource {

    private final BukkitSource source;

    public void sendMsg(String message) {
        source.origin().sendRichMessage(message);
    }
}
