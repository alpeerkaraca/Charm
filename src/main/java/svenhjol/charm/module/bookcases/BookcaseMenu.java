package svenhjol.charm.module.bookcases;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import svenhjol.charm.menu.CharmContainerMenu;

public class BookcaseMenu extends CharmContainerMenu {
    public BookcaseMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(BookcaseBlockEntity.SIZE));
    }

    public BookcaseMenu(int syncId, Inventory playerInventory, Container container) {
        super(Bookcases.MENU, syncId, playerInventory, container);
        var index = 0;

        // Container's inventory slots. TODO: fix graphics.
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new BookcaseSlot(container, index++, 62 + (x * 18), 18 + (y * 18)));
            }
        }

        index = 9; // Start of player inventory.

        // Player's main inventory slots.
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                addSlot(new Slot(playerInventory, index++, 8 + c * 18, 86 + r * 18));
            }
        }

        // Player's hotbar.
        for (int i = 0; i < 9; ++i) {
            addSlot(new Slot(playerInventory, i, 8 + (i * 18), 144));
        }
    }

    // TODO: override clicked and do advancement.
}
