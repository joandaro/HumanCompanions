package com.github.justinwon777.humancompanions.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "humancompanions", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionOvernightCooking {
    private CompanionOvernightCooking() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getNewTime() <= level.getDayTime()) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AbstractHumanCompanionEntity companion
                    && companion.isAlive() && companion.isTame()) {
                cookInventory(companion.inventory, level);
            }
        }
    }

    private static void cookInventory(SimpleContainer inventory, ServerLevel level) {
        boolean hasCampfire = false;
        boolean hasFuel = false;
        ItemStack[] originalFoods = new ItemStack[inventory.getContainerSize()];

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            hasCampfire |= stack.is(Items.CAMPFIRE) || stack.is(Items.SOUL_CAMPFIRE);
            hasFuel |= isFuel(stack);
            if (stack.isEdible()) {
                originalFoods[slot] = stack.copy();
            }
        }
        if (!hasCampfire && !hasFuel) {
            return;
        }

        long remainingHeat = 0;
        for (int slot = 0; slot < originalFoods.length; slot++) {
            ItemStack original = originalFoods[slot];
            ItemStack stack = inventory.getItem(slot);
            if (original == null || !ItemStack.isSameItemSameTags(original, stack)) {
                continue;
            }

            SimpleContainer input = new SimpleContainer(stack.copy());
            AbstractCookingRecipe recipe = null;
            if (hasCampfire) {
                recipe = level.getRecipeManager()
                        .getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level).orElse(null);
            }
            boolean useFuel = recipe == null;
            if (useFuel && CompanionSmelting.canSmelt(stack, level)) {
                recipe = level.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, input, level).orElse(null);
            }
            if (recipe == null) {
                continue;
            }

            int maximum = Math.min(original.getCount(), stack.getCount());
            if (useFuel) {
                long heat = availableHeat(inventory, remainingHeat);
                maximum = (int) Math.min(maximum, heat / Math.max(1, recipe.getCookingTime()));
            }

            for (int amount = maximum; amount > 0; amount--) {
                SimpleContainer trial = copyInventory(inventory);
                long heat = cookBatch(trial, slot, amount, recipe, useFuel, remainingHeat, level);
                if (heat >= 0) {
                    commitInventory(inventory, trial);
                    remainingHeat = heat;
                    break;
                }
            }
        }
    }

    private static long cookBatch(SimpleContainer trial, int slot, int amount,
                                  AbstractCookingRecipe recipe, boolean useFuel,
                                  long heat, ServerLevel level) {
        List<ItemStack> results = new ArrayList<>();
        int cookingTime = Math.max(1, recipe.getCookingTime());

        for (int count = 0; count < amount; count++) {
            SimpleContainer input = new SimpleContainer(trial.getItem(slot).copy());
            if (!recipe.matches(input, level)) {
                return -1;
            }
            ItemStack result = recipe.assemble(input, level.registryAccess());
            if (!result.isEdible()) {
                return -1;
            }
            if (useFuel) {
                heat = consumeFuel(trial, heat, cookingTime);
                if (heat < 0) {
                    return -1;
                }
            }

            trial.removeItem(slot, 1);
            results.add(result.copy());
        }

        for (ItemStack result : results) {
            while (!result.isEmpty()) {
                int limit = Math.min(result.getMaxStackSize(), trial.getMaxStackSize());
                ItemStack part = result.split(limit);
                if (!trial.addItem(part).isEmpty()) {
                    return -1;
                }
            }
        }
        return heat;
    }

    private static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    private static long availableHeat(SimpleContainer inventory, long heat) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isFuel(stack)) {
                int burnTime = Math.max(0, ForgeHooks.getBurnTime(stack, RecipeType.SMELTING));
                heat += (long) burnTime * stack.getCount();
            }
        }
        return heat;
    }

    private static long consumeFuel(SimpleContainer inventory, long heat, int needed) {
        for (int slot = 0; slot < inventory.getContainerSize() && heat < needed; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isFuel(stack)) {
                continue;
            }
            int burnTime = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
            if (burnTime <= 0) {
                continue;
            }
            int count = (int) Math.min(stack.getCount(), (needed - heat + burnTime - 1) / burnTime);
            inventory.removeItem(slot, count);
            heat += (long) count * burnTime;
        }
        return heat >= needed ? heat - needed : -1;
    }

    private static SimpleContainer copyInventory(SimpleContainer inventory) {
        SimpleContainer copy = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            copy.setItem(slot, inventory.getItem(slot).copy());
        }
        return copy;
    }

    private static void commitInventory(SimpleContainer inventory, SimpleContainer trial) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack before = inventory.getItem(slot);
            ItemStack after = trial.getItem(slot);
            if (ItemStack.matches(before, after)) {
                continue;
            }
            if (ItemStack.isSameItemSameTags(before, after)) {
                before.setCount(after.getCount());
            } else {
                before.setCount(0);
                inventory.setItem(slot, after);
            }
        }
        inventory.setChanged();
    }
}
