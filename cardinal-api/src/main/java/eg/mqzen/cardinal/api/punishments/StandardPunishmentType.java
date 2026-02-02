package eg.mqzen.cardinal.api.punishments;

public enum StandardPunishmentType implements PunishmentType {

    BAN("ban", true),
    MUTE("mute", true),
    KICK("kick", false),
    WARN("warn", true);

    private final String id;
    private final boolean memoryWorthy;
    StandardPunishmentType(String id, boolean memoryWorthy) {
        this.id = id;
        this.memoryWorthy = memoryWorthy;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isMemoryWorthy() {
        return memoryWorthy;
    }

    @Override
    public boolean supportsDuration() {
        return this != KICK;
    }

}
