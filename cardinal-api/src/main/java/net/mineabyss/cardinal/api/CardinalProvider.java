package net.mineabyss.cardinal.api;

public final class CardinalProvider {

    private static CardinalAPI api;

    public static void load(CardinalAPI api) {
        if(CardinalProvider.api != null) {
            throw new IllegalStateException("API is already loaded!");
        }
        CardinalProvider.api = api;
    }

    public static CardinalAPI provide() {
        return api;
    }

}
