package com.github.justinwon777.humancompanions.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "humancompanions", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionSmelting {
    private static Set<Item> simpleInputs = Set.of();
    private static List<SmeltingRecipe> conditionalRecipes = List.of();

    private CompanionSmelting() {
    }

    public static boolean canSmelt(ItemStack stack, Level level) {
        if (simpleInputs.contains(stack.getItem())) {
            return true;
        }
        if (conditionalRecipes.isEmpty()) {
            return false;
        }

        SimpleContainer input = new SimpleContainer(stack);
        for (SmeltingRecipe recipe : conditionalRecipes) {
            if (recipe.matches(input, level)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        rebuild(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            rebuild(event.getPlayerList().getServer().getRecipeManager());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        simpleInputs = Set.of();
        conditionalRecipes = List.of();
    }

    private static void rebuild(RecipeManager recipeManager) {
        Set<Item> newInputs = new HashSet<>();
        List<SmeltingRecipe> newConditionalRecipes = new ArrayList<>();

        for (SmeltingRecipe recipe : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (recipe.getClass() == SmeltingRecipe.class
                    && ingredients.size() == 1
                    && ingredients.get(0).getClass() == Ingredient.class) {
                for (ItemStack stack : ingredients.get(0).getItems()) {
                    if (stack.isEdible()) {
                        newInputs.add(stack.getItem());
                    }
                }
            } else {
                newConditionalRecipes.add(recipe);
            }
        }

        simpleInputs = Set.copyOf(newInputs);
        conditionalRecipes = List.copyOf(newConditionalRecipes);
    }
}
