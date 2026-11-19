package com.github.justinwon777.humancompanions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "humancompanions", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionOvernightCooking {
    private static final int CAMPFIRE_RANGE = 7;

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
                cookInventory(companion, level);
            }
        }
    }

    private static void cookInventory(AbstractHumanCompanionEntity companion, ServerLevel level) {
        SimpleContainer inventory = companion.inventory;
        if (!inventory.hasAnyOf(Set.of(Items.CAMPFIRE, Items.SOUL_CAMPFIRE))
                && !hasNearbyCampfire(companion, level)) {
            return;
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack result = findResult(stack, level);
            if (result.isEmpty()) {
                continue;
            }

            int amount = stack.getCount();
            SimpleContainer trial = copyInventory(inventory);
            trial.removeItem(slot, amount);

            ItemStack output = result.copy();
            output.setCount(result.getCount() * amount);
            if (ItemHandlerHelper.insertItemStacked(new InvWrapper(trial), output, false).isEmpty()) {
                commitInventory(inventory, trial);
            }
        }
    }

    private static boolean hasNearbyCampfire(AbstractHumanCompanionEntity companion, ServerLevel level) {
        BlockPos origin = companion.blockPosition();
        Vec3 eyes = companion.getEyePosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-CAMPFIRE_RANGE, -CAMPFIRE_RANGE, -CAMPFIRE_RANGE),
                origin.offset(CAMPFIRE_RANGE, CAMPFIRE_RANGE, CAMPFIRE_RANGE))) {
            if (pos.distToCenterSqr(companion.position()) > CAMPFIRE_RANGE * CAMPFIRE_RANGE
                    || !level.hasChunkAt(pos)
                    || !CampfireBlock.isLitCampfire(level.getBlockState(pos))) {
                continue;
            }
            var hit = level.clip(new ClipContext(eyes, Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, companion));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack findResult(ItemStack stack, ServerLevel level) {
        for (CampfireCookingRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CAMPFIRE_COOKING)) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (recipe.getClass() != CampfireCookingRecipe.class
                    || ingredients.size() != 1
                    || ingredients.get(0).getClass() != Ingredient.class) {
                continue;
            }
            if (ingredients.get(0).test(stack)) {
                return recipe.getResultItem(level.registryAccess());
            }
        }
        return ItemStack.EMPTY;
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
