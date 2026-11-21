package com.github.justinwon777.humancompanions.container;

import com.github.justinwon777.humancompanions.compat.CompanionCurioSlotData;
import com.github.justinwon777.humancompanions.compat.CompanionCurios;
import com.github.justinwon777.humancompanions.entity.AbstractHumanCompanionEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CompanionContainer extends AbstractContainerMenu {
    private static final int BAG_END = 27;
    private static final int PLAYER_END = BAG_END + 36;

    private final Container container;
    private final AbstractHumanCompanionEntity companion;
    private final List<CompanionCurioSlotData> curioSlots;
    private int curioPage;

    /** Retained for existing callers that only need the original bag. */
    public CompanionContainer(int id, Inventory playerInventory, Container container) {
        this(id, playerInventory, container, null, List.of());
    }

    /** Server constructor: discover the real companion's slot layout. */
    public CompanionContainer(int id, Inventory playerInventory, AbstractHumanCompanionEntity companion) {
        this(id, playerInventory, companion, CompanionCurios.getSlots(companion));
    }

    /** Client constructor: use the exact ordered slot addresses sent by the server. */
    public CompanionContainer(int id, Inventory playerInventory, AbstractHumanCompanionEntity companion,
                              List<CompanionCurioSlotData> curioSlots) {
        this(id, playerInventory, companion.inventory, companion, curioSlots);
    }

    private CompanionContainer(int id, Inventory playerInventory, Container container,
                               AbstractHumanCompanionEntity companion,
                               List<CompanionCurioSlotData> curioSlots) {
        super(null, id);
        checkContainerSize(container, BAG_END);
        this.container = container;
        this.companion = companion;
        this.curioSlots = List.copyOf(curioSlots);
        container.startOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(container, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 85 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 143));
        }

        // Append equipment so the original bag/player slot indices remain unchanged.
        for (int i = 0; i < this.curioSlots.size(); i++) {
            final int page = i / CompanionCurios.PAGE_SIZE;
            int position = i % CompanionCurios.PAGE_SIZE;
            int columns = getCurioColumns(page);
            int panelWidth = 14 + columns * 18;
            addSlot(CompanionCurios.createSlot(companion, this.curioSlots.get(i),
                    -CompanionCurios.PANEL_GAP - panelWidth + 8 + (position % columns) * 18,
                    8 + getCurioHeaderHeight() + (position / columns) * 18,
                    () -> getCurioPage() == page));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (!container.stillValid(player)) {
            return false;
        }
        if (companion == null) {
            return true;
        }
        return companion.isAlive() && !companion.isRemoved()
                && companion.level() == player.level() && companion.isOwnedBy(player)
                && companion.distanceToSqr(player) <= 64.0D
                && (player.level().isClientSide()
                || curioSlots.equals(CompanionCurios.getSlots(companion)));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index >= PLAYER_END) {
            moved = moveItemStackTo(stack, BAG_END, PLAYER_END, true);
            if (!moved) {
                moved = moveItemStackTo(stack, 0, BAG_END, false);
            }
        } else if (index < BAG_END) {
            moved = moveToCurios(stack);
            if (!moved) {
                moved = moveItemStackTo(stack, BAG_END, PLAYER_END, true);
            }
        } else {
            moved = moveToCurios(stack);
            if (!moved) {
                moved = moveItemStackTo(stack, 0, BAG_END, false);
            }
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    private boolean moveToCurios(ItemStack stack) {
        return hasCurios() && CompanionCurios.isApproved(stack)
                && moveItemStackTo(stack, PLAYER_END, slots.size(), false);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    public int getRowCount() {
        return 3;
    }

    public List<CompanionCurioSlotData> getCurioSlots() {
        return curioSlots;
    }

    public boolean hasCurios() {
        return !curioSlots.isEmpty();
    }

    public int getCurioPage() {
        return curioPage;
    }

    public int getCurioPageCount() {
        return Math.max(1, (curioSlots.size() + CompanionCurios.PAGE_SIZE - 1) / CompanionCurios.PAGE_SIZE);
    }

    public void setCurioPage(int page) {
        curioPage = Math.max(0, Math.min(page, getCurioPageCount() - 1));
    }

    private int getCurioSlotCount(int page) {
        int first = page * CompanionCurios.PAGE_SIZE;
        return Math.max(0, Math.min(CompanionCurios.PAGE_SIZE, curioSlots.size() - first));
    }

    private int getCurioColumns(int page) {
        int count = getCurioSlotCount(page);
        return Math.max(CompanionCurios.MIN_COLUMNS,
                (count + CompanionCurios.MAX_ROWS - 1) / CompanionCurios.MAX_ROWS);
    }

    public int getCurioColumns() {
        return hasCurios() ? getCurioColumns(curioPage) : 0;
    }

    public int getCurioRows() {
        if (!hasCurios()) {
            return 0;
        }
        int columns = getCurioColumns();
        return (getCurioSlotCount(curioPage) + columns - 1) / columns;
    }

    public int getCurioHeaderHeight() {
        return getCurioPageCount() > 1 ? 8 : 0;
    }

    public int getCurioPanelWidth() {
        return hasCurios() ? 14 + getCurioColumns() * 18 : 0;
    }

    public int getMaximumCurioPanelWidth() {
        return hasCurios() ? 14 + getCurioColumns(0) * 18 : 0;
    }

    public int getCurioPanelHeight() {
        return hasCurios() ? 14 + getCurioRows() * 18 + getCurioHeaderHeight() : 0;
    }

    public CompanionCurioSlotData getCurioSlotData(int menuSlotIndex) {
        int index = menuSlotIndex - PLAYER_END;
        return index >= 0 && index < curioSlots.size() ? curioSlots.get(index) : null;
    }
}
