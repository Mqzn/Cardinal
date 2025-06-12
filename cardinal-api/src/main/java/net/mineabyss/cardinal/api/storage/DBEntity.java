package net.mineabyss.cardinal.api.storage;

import org.jetbrains.annotations.NotNull;

public interface DBEntity<ID> {

    @NotNull ID getEntityID();
}
