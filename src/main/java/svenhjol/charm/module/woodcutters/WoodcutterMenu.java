package svenhjol.charm.module.woodcutters;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

// much copypasta from StonecutterMenu
public class WoodcutterMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess context;
    private final DataSlot selectedRecipe;
    private final Level level;
    private List<WoodcuttingRecipe> availableRecipes;
    private ItemStack inputStack;
    private long lastTakeTime;
    final Slot inputSlot;
    final Slot outputSlot;
    private Runnable contentsChangedListener;
    public final Container input;
    private final ResultContainer output;

    public WoodcutterMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public WoodcutterMenu(int syncId, Inventory playerInventory, final ContainerLevelAccess context) {
        super(Woodcutters.MENU, syncId);
        this.selectedRecipe = DataSlot.standalone();
        this.availableRecipes = Lists.newArrayList();
        this.inputStack = ItemStack.EMPTY;
        this.contentsChangedListener = () -> {
        };
        this.input = new SimpleContainer(1) {
            public void setChanged() {
                super.setChanged();
                WoodcutterMenu.this.slotsChanged(this);
                WoodcutterMenu.this.contentsChangedListener.run();
            }
        };
        this.output = new ResultContainer();
        this.context = context;
        this.level = playerInventory.player.level;
        this.inputSlot = this.addSlot(new Slot(this.input, 0, 20, 33));
        this.outputSlot = this.addSlot(new Slot(this.output, 1, 143, 33) {
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            public void onTake(Player player, ItemStack stack) {
                stack.onCraftedBy(player.level, player, stack.getCount());
                WoodcutterMenu.this.output.awardUsedRecipes(player);
                ItemStack itemStack = WoodcutterMenu.this.inputSlot.remove(1);
                if (!itemStack.isEmpty()) {
                    WoodcutterMenu.this.populateResult();
                }

                context.execute((level, blockPos) -> {
                    long l = level.getGameTime();
                    if (WoodcutterMenu.this.lastTakeTime != l) {
                        level.playSound(null, blockPos, Woodcutters.USE_SOUND, SoundSource.BLOCKS, 0.4F, 1.0F);
                        WoodcutterMenu.this.lastTakeTime = l;
                    }
                });
            }
        });

        int k;
        for(k = 0; k < 3; ++k) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + k * 9 + 9, 8 + j * 18, 84 + k * 18));
            }
        }

        for(k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlot(this.selectedRecipe);
    }

    @Environment(EnvType.CLIENT)
    public int getSelectedRecipe() {
        return this.selectedRecipe.get();
    }

    @Environment(EnvType.CLIENT)
    public List<WoodcuttingRecipe> getAvailableRecipes() {
        return this.availableRecipes;
    }

    @Environment(EnvType.CLIENT)
    public int getAvailableRecipeCount() {
        return this.availableRecipes.size();
    }

    @Environment(EnvType.CLIENT)
    public boolean canCraft() {
        return this.inputSlot.hasItem() && !this.availableRecipes.isEmpty();
    }

    public boolean stillValid(Player player) {
        return stillValid(this.context, player, Woodcutters.WOODCUTTER);
    }

    public boolean clickMenuButton(Player player, int id) {
        if (this.method_30160(id)) {
            this.selectedRecipe.set(id);
            this.populateResult();
        }

        return true;
    }

    private boolean method_30160(int i) {
        return i >= 0 && i < this.availableRecipes.size();
    }

    public void slotsChanged(Container inventory) {
        ItemStack itemStack = this.inputSlot.getItem();
        if (itemStack.getItem() != this.inputStack.getItem()) {
            this.inputStack = itemStack.copy();
            this.updateInput(inventory, itemStack);
        }

    }

    private void updateInput(Container input, ItemStack stack) {
        this.availableRecipes.clear();
        this.selectedRecipe.set(-1);
        this.outputSlot.set(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            this.availableRecipes = this.level.getRecipeManager().getRecipesFor(Woodcutters.RECIPE_TYPE, input, this.level);
        }

    }

    private void populateResult() {
        if (!this.availableRecipes.isEmpty() && this.method_30160(this.selectedRecipe.get())) {
            WoodcuttingRecipe woodcuttingRecipe = this.availableRecipes.get(this.selectedRecipe.get());
            this.output.setRecipeUsed(woodcuttingRecipe);
            this.outputSlot.set(woodcuttingRecipe.assemble(this.input));
        } else {
            this.outputSlot.set(ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    public MenuType<?> getType() {
        return Woodcutters.MENU;
    }

    @Environment(EnvType.CLIENT)
    public void setContentsChangedListener(Runnable runnable) {
        this.contentsChangedListener = runnable;
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.output && super.canTakeItemForPickAll(stack, slot);
    }

    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            Item item = itemStack2.getItem();
            itemStack = itemStack2.copy();
            if (index == 1) {
                item.onCraftedBy(itemStack2, player.level, player);
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.level.getRecipeManager().getRecipeFor(Woodcutters.RECIPE_TYPE, new SimpleContainer(new ItemStack[]{itemStack2}), this.level).isPresent()) {
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 2 && index < 29) {
                if (!this.moveItemStackTo(itemStack2, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 29 && index < 38 && !this.moveItemStackTo(itemStack2, 2, 29, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
            this.broadcastChanges();
        }

        return itemStack;
    }

    public void removed(Player player) {
        super.removed(player);
        this.output.removeItemNoUpdate(1);
        this.context.execute((level, blockPos) -> {
            this.clearContainer(player, this.input);
        });
    }
}
