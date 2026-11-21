package com.github.justinwon777.humancompanions.compat;

import com.github.justinwon777.humancompanions.entity.AbstractHumanCompanionEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class CuriosIntegration {
    private CuriosIntegration() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(CuriosIntegration.class);
    }

    // Also covers normal Curios insertion checks and saved equipment validation.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void checkEquip(CurioEquipEvent event) {
        if (event.getEntity() instanceof AbstractHumanCompanionEntity
                && !CompanionCurios.isApproved(event.getStack())) {
            event.setResult(Event.Result.DENY);
        }
    }

    // Runs before Curios' NORMAL-priority ticking handler.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void recheckEquipment(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity()
                instanceof AbstractHumanCompanionEntity companion)) {
            return;
        }

        CuriosApi.getCuriosInventory(companion).ifPresent(inventory ->
                inventory.getCurios().values().forEach(handler -> {
                    returnUnapproved(companion, handler.getStacks());
                    returnUnapproved(companion, handler.getCosmeticStacks());
                }));
    }

    private static void returnUnapproved(
            AbstractHumanCompanionEntity companion,
            IDynamicStackHandler stacks) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);

            if (!stack.isEmpty() && !CompanionCurios.isApproved(stack)) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);

                if (!companion.level().isClientSide()) {
                    ItemStack remaining = companion.inventory.addItem(stack);

                    if (!remaining.isEmpty()) {
                        companion.spawnAtLocation(remaining);
                    }
                }

                // Leave previousStacks intact so Curios removes old modifiers
                // and performs normal cleanup for formerly approved equipment.
            }
        }
    }

    public static List<CompanionCurioSlotData> getSlots(
            AbstractHumanCompanionEntity companion) {
        List<CompanionCurioSlotData> slots = new ArrayList<>();

        CuriosApi.getCuriosInventory(companion).ifPresent(inventory ->
                inventory.getCurios().entrySet().stream()
                        //.sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            int size = entry.getValue().getStacks().getSlots();

                            for (int i = 0; i < size; i++) {
                                if (slots.size()
                                        >= CompanionCurioSlotData.MAX_SLOTS) {
                                    throw new IllegalStateException(
                                            "Too many companion Curios slots"
                                                    + " (maximum 256)");
                                }

                                slots.add(new CompanionCurioSlotData(
                                        entry.getKey(), i));
                            }
                        }));

        return List.copyOf(slots);
    }

    public static Slot createSlot(
            AbstractHumanCompanionEntity companion,
            CompanionCurioSlotData address,
            int x,
            int y,
            BooleanSupplier visible) {
        return new CompanionSlot(companion, address, x, y, visible);
    }

    private static IDynamicStackHandler resolve(
            AbstractHumanCompanionEntity companion,
            CompanionCurioSlotData address) {
        return CuriosApi.getCuriosInventory(companion).resolve()
                .map(inventory ->
                        inventory.getCurios().get(address.identifier()))
                .map(handler -> handler.getStacks())
                .filter(handler -> address.index() < handler.getSlots())
                .orElse(null);
    }

    private static final class CompanionSlot extends SlotItemHandler {
        private final AbstractHumanCompanionEntity companion;
        private final BooleanSupplier visible;

        private CompanionSlot(
                AbstractHumanCompanionEntity companion,
                CompanionCurioSlotData address,
                int x,
                int y,
                BooleanSupplier visible) {
            super(new SlotInventory(companion, address), 0, x, y);
            this.companion = companion;
            this.visible = visible;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Reject before invoking accessory-provided validation.
            return CompanionCurios.isApproved(stack)
                    && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return companion.isOwnedBy(player)
                    && super.mayPickup(player);
        }

        @Override
        public boolean isActive() {
            // Pagination affects only the client's display.
            return !companion.level().isClientSide()
                    || visible.getAsBoolean();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    private static final class SlotInventory
            implements IItemHandlerModifiable {
        private final AbstractHumanCompanionEntity companion;
        private final CompanionCurioSlotData address;

        // Client menu synchronization uses this view.
        // The server always uses the real Curios inventory.
        private final ItemStackHandler clientView =
                new ItemStackHandler(1);

        private SlotInventory(
                AbstractHumanCompanionEntity companion,
                CompanionCurioSlotData address) {
            this.companion = companion;
            this.address = address;
        }

        private void checkIndex(int slot) {
            if (slot != 0) {
                throw new IndexOutOfBoundsException(
                        "Companion Curios slot wrapper has one slot");
            }
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            checkIndex(slot);

            if (companion.level().isClientSide()) {
                return clientView.getStackInSlot(0);
            }

            IDynamicStackHandler handler = resolve(companion, address);

            return handler == null
                    ? ItemStack.EMPTY
                    : handler.getStackInSlot(address.index());
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            checkIndex(slot);

            if (companion.level().isClientSide()) {
                clientView.setStackInSlot(0, stack);
                return;
            }

            IDynamicStackHandler handler = resolve(companion, address);

            if (handler == null) {
                if (!stack.isEmpty()) {
                    throw new IllegalStateException(
                            "Companion Curios layout changed;"
                                    + " reopen inventory");
                }
                return;
            }

            if (!stack.isEmpty()
                    && (!CompanionCurios.isApproved(stack)
                    || stack.getCount() != 1
                    || !handler.isItemValid(address.index(), stack))) {
                throw new IllegalArgumentException(
                        "Invalid companion accessory");
            }

            handler.setStackInSlot(address.index(), stack);
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            checkIndex(slot);

            if (stack.isEmpty() || !isItemValid(slot, stack)) {
                return stack;
            }

            if (companion.level().isClientSide()) {
                return clientView.insertItem(0, stack, simulate);
            }

            IDynamicStackHandler handler = resolve(companion, address);

            if (handler == null
                    || !handler.getStackInSlot(address.index()).isEmpty()) {
                return stack;
            }

            ItemStack one = stack.copyWithCount(1);
            ItemStack rejected =
                    handler.insertItem(address.index(), one, simulate);

            return rejected.isEmpty()
                    ? stack.copyWithCount(stack.getCount() - 1)
                    : stack;
        }

        @Override
        public ItemStack extractItem(
                int slot, int amount, boolean simulate) {
            checkIndex(slot);

            if (companion.level().isClientSide()) {
                return clientView.extractItem(0, amount, simulate);
            }

            IDynamicStackHandler handler = resolve(companion, address);

            return handler == null
                    ? ItemStack.EMPTY
                    : handler.extractItem(
                    address.index(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            checkIndex(slot);
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            checkIndex(slot);

            if (!CompanionCurios.isApproved(stack)) {
                return false;
            }

            // Server decides the actual Curios slot validation.
            if (companion.level().isClientSide()) {
                return true;
            }

            IDynamicStackHandler handler = resolve(companion, address);

            return handler != null
                    && handler.isItemValid(address.index(), stack);
        }
    }
}