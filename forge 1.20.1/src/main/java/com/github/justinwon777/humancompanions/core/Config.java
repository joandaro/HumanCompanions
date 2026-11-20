package com.github.justinwon777.humancompanions.core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class Config {

    public static ForgeConfigSpec.IntValue AVERAGE_HOUSE_SEPARATION;
    public static ForgeConfigSpec.BooleanValue FRIENDLY_FIRE_COMPANIONS;
    public static ForgeConfigSpec.BooleanValue FRIENDLY_FIRE_PLAYER;
    public static ForgeConfigSpec.BooleanValue FALL_DAMAGE;
    public static ForgeConfigSpec.BooleanValue SPAWN_ARMOR;
    public static ForgeConfigSpec.BooleanValue SPAWN_WEAPON;
    public static ForgeConfigSpec.IntValue BASE_HEALTH;
    public static ForgeConfigSpec.BooleanValue LOW_HEALTH_FOOD;
    public static ForgeConfigSpec.BooleanValue CREEPER_WARNING;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_PICKUP_ITEMS;

    public static void register() {
        registerCommonConfig();
    }

    private static void registerCommonConfig() {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("Settings for world gen (Doesn't work in 1.18.2 and beyond. Use datapacks instead.)").push("World" +
                " Gen");
        AVERAGE_HOUSE_SEPARATION = COMMON_BUILDER
                .comment("Average chunk separation between companion houses")
                .defineInRange("averageHouseSeparation", 20, 11, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();
        COMMON_BUILDER.push("Companion config");
        FRIENDLY_FIRE_COMPANIONS = COMMON_BUILDER
                .comment("Toggles friendly fire between companions")
                .define("friendlyFireCompanions", false);
        FRIENDLY_FIRE_PLAYER = COMMON_BUILDER
                .comment("Toggles friendly fire between player and companion")
                .define("friendlyFirePlayer", true);
        FALL_DAMAGE = COMMON_BUILDER
                .comment("Toggles fall damage for companions")
                .define("fallDamage", true);
        SPAWN_ARMOR = COMMON_BUILDER
                .comment("Toggles whether companions spawn with armor")
                .define("spawnArmor", true);
        SPAWN_WEAPON = COMMON_BUILDER
                .comment("Toggles whether companions spawn with a weapon")
                .define("spawnWeapon", true);
        BASE_HEALTH = COMMON_BUILDER
                .comment("Sets the base health of each companion. Companions spawn with up to +-4 from the base health")
                .defineInRange("baseHealth", 20, 5, Integer.MAX_VALUE);
        LOW_HEALTH_FOOD = COMMON_BUILDER
                .comment("Toggles whether companions ask for food if their health goes below half.")
                .define("lowHealthFood", true);
        CREEPER_WARNING = COMMON_BUILDER
                .comment("Toggles whether companions alert you if a creeper is nearby.")
                .define("creeperWarning", true);
        EXTRA_PICKUP_ITEMS = COMMON_BUILDER
                .comment("Extra item IDs allowed for pickup. Use namespace:item_id.")
                .defineListAllowEmpty("extraPickupItems", List.of(
                        "minecraft:coal",
                        "minecraft:charcoal",
                        "minecraft:gunpowder",
                        "minecraft:bone",
                        "minecraft:arrow",
                        "minecraft:string",
                        "minecraft:leather",
                        "minecraft:egg",
                        "minecraft:rabbit_hide",
                        "minecraft:rabbit_foot",
                        "minecraft:white_wool",
                        "minecraft:black_wool",
                        "minecraft:brown_wool",
                        "minecraft:gray_wool",
                        "minecraft:light_gray_wool",
                        "minecraft:feather",
                        "minecraft:spider_eye",
                        "minecraft:slime_ball",
                        "minecraft:ender_eye",
                        "minecraft:blaze_rod",
                        "minecraft:wither_skeleton_skull",
                        "minecraft:ghast_tear",
                        "minecraft:netherite_scrap"
                ), value -> value instanceof String id
                        && id.contains(":")
                        && ResourceLocation.tryParse(id) != null);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_BUILDER.build());
    }
}
