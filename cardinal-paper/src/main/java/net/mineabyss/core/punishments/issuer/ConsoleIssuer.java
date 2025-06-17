package net.mineabyss.core.punishments.issuer;

import net.kyori.adventure.text.Component;
import net.mineabyss.cardinal.api.punishments.IssuerType;
import net.mineabyss.cardinal.api.punishments.PunishmentIssuer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ConsoleIssuer implements PunishmentIssuer {

    private final static UUID EMPTY_UUID = new UUID(0, 0);

    private static ConsoleIssuer INSTANCE;

    public static ConsoleIssuer get() {
        if(INSTANCE == null) {
            INSTANCE = new ConsoleIssuer();
        }
        return INSTANCE;
    }

    private ConsoleIssuer() {
        if(INSTANCE != null) {
            throw new IllegalStateException("ConsoleIssuer is a singleton and cannot be instantiated multiple times.");
        }
    }


    /**
     * Returns the name of the issuer.
     *
     * @return the name of the issuer
     */
    @NotNull @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    /**
     * Returns the unique identifier for this issuer.
     *
     * @return the unique identifier as a UUID
     */
    @Override
    public @NotNull UUID getUniqueId() {
        return EMPTY_UUID;
    }

    /**
     * Returns the type of this issuer.
     *
     * @return the type of the issuer
     */
    @Override
    public @NotNull IssuerType getType() {
        return IssuerType.CONSOLE;
    }

    @Override
    public void sendMsg(String msg) {
        Bukkit.getConsoleSender().sendRichMessage(msg);
    }

    @Override
    public void sendMsg(Component component) {
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
