package eg.mqzen.cardinal.punishments.gui;

import eg.mqzen.lib.gui.base.Content;
import eg.mqzen.lib.gui.base.pagination.FillRange;
import eg.mqzen.lib.gui.base.pagination.Page;
import eg.mqzen.lib.gui.base.pagination.Pagination;
import eg.mqzen.lib.gui.misc.Capacity;
import eg.mqzen.lib.gui.misc.DataRegistry;
import eg.mqzen.lib.gui.misc.itembuilder.LegacyItemBuilder;
import eg.mqzen.lib.gui.titles.MenuTitle;
import eg.mqzen.lib.gui.titles.MenuTitles;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class HistoryPage extends Page {


    public HistoryPage() {

    }

    @Override
    public FillRange getFillRange(Capacity capacity, Player player) {
        return FillRange.start(capacity);
    }

    @Override
    public ItemStack nextPageItem(Player player) {
        return LegacyItemBuilder.legacy(Material.ARROW).setDisplay("&aNext Page ->").build();
    }

    @Override
    public ItemStack previousPageItem(Player player) {
        return LegacyItemBuilder.legacy(Material.ARROW).setDisplay("&e<- Previous Page").build();
    }

    @Override
    public String getName() {
        return "history-gui";
    }

    @Override
    public @NotNull MenuTitle getTitle(DataRegistry dataRegistry, Player player) {
        int index = dataRegistry.getData("index");
        Pagination pagination = dataRegistry.getData("pagination");
        int max = pagination.getMaximumPages();
        return MenuTitles.createLegacy("&cHistory GUI (" + (index+1) + "/" + max + ")");
    }

    @Override
    public @NotNull Capacity getCapacity(DataRegistry dataRegistry, Player player) {
        return Capacity.ofRows(1);
    }

    @Override
    public @NotNull Content getContent(DataRegistry dataRegistry, Player player, Capacity capacity) {
        return Content.builder(capacity)
                .build();
    }
}
