package com.github.justinwon777.humancompanions.compat;

import com.github.justinwon777.humancompanions.entity.AbstractHumanCompanionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class CompanionCurios {
    public static final int PAGE_SIZE = 48;
    public static final int MIN_COLUMNS = 2;
    public static final int MAX_ROWS = 8;
    public static final int PANEL_GAP = 3;

    private CompanionCurios() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static void register() {
        CompanionCuriosConfig.register();
        if (isLoaded()) {
            CuriosIntegration.register();
        }
    }

    public static boolean isApproved(ItemStack stack) {
        return isApproved(stack, CompanionCuriosConfig.getRules());
    }

    private static boolean isApproved(ItemStack stack, CompanionCuriosConfig.Rules rules) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && rules.allows(id);
    }

    public static List<CompanionCurioSlotData> getSlots(AbstractHumanCompanionEntity companion) {
        if (!isLoaded()) {
            return List.of();
        }
        CompanionCuriosConfig.Rules rules = CompanionCuriosConfig.getRules();
        if (rules.mode() == CompanionCuriosConfig.ListMode.WHITELIST
                && !rules.hasRegisteredItems()) {
            return List.of();
        }
        return CuriosIntegration.getSlots(companion);
    }

    public static Slot createSlot(AbstractHumanCompanionEntity companion,
                                  CompanionCurioSlotData address, int x, int y,
                                  BooleanSupplier visible) {
        if (!isLoaded()) {
            throw new IllegalStateException("Cannot create companion Curios slots without Curios");
        }
        return CuriosIntegration.createSlot(companion, address, x, y, visible);
    }
}
