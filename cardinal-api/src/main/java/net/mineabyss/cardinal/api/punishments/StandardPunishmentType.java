package net.mineabyss.cardinal.api.punishments;

public enum StandardPunishmentType implements PunishmentType {

    BAN("ban"),
    MUTE("mute"),
    KICK("kick"),
    WARN("warn");

    private final String id;

    StandardPunishmentType(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

}
