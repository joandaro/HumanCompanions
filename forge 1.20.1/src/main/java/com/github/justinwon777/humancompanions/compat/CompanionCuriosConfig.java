package com.github.justinwon777.humancompanions.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CompanionCuriosConfig {
    public enum ListMode {
        WHITELIST,
        BLACKLIST
    }

    public static final ForgeConfigSpec.EnumValue<ListMode> LIST_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEMS;
    private static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Curios");
        LIST_MODE = builder
                .comment("WHITELIST allows listed item IDs; BLACKLIST blocks listed item IDs.",
                        "An empty whitelist hides companion accessory slots.",
                        "An empty blacklist allows all available accessories, including untested ones.",
                        "Note that some curio items don't work on non-player entities",
                        "and could potentially crash the game or even corrupt a save when equipped by one.")
                .defineEnum("listMode", ListMode.WHITELIST);
        ITEMS = builder
                .comment("Companion accessory item IDs, for example [\"modid:item1\",\"modid:item2\"].",
                        "Unknown item IDs are allowed.")
                .defineListAllowEmpty("items", List.<String>of(),
                        value -> value instanceof String id
                                && id.contains(":")
                                && ResourceLocation.tryParse(id) != null);
        builder.pop();
        SPEC = builder.build();
    }

    public record Rules(ListMode mode, Set<ResourceLocation> items) {
        public Rules {
            items = Set.copyOf(items);
        }

        public boolean allows(ResourceLocation itemId) {
            return mode == ListMode.WHITELIST
                    ? items.contains(itemId)
                    : !items.contains(itemId);
        }

        public boolean hasRegisteredItems() {
            ResourceLocation air = new ResourceLocation("minecraft", "air");
            return items.stream().anyMatch(id -> !id.equals(air)
                    && ForgeRegistries.ITEMS.containsKey(id));
        }
    }

    private record CachedRules(ListMode mode, List<? extends String> source, Rules rules) {}
    private static volatile CachedRules cachedRules;

    private CompanionCuriosConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC,
                "humancompanions-curios.toml");
    }

    public static Rules getRules() {
        ListMode mode = LIST_MODE.get();
        List<? extends String> source = ITEMS.get();
        CachedRules cached = cachedRules;
        // Forge replaces a config value's cached list on file reload or server sync.
        if (cached == null || cached.mode() != mode || cached.source() != source) {
            Set<ResourceLocation> ids = new LinkedHashSet<>();
            for (String text : source) {
                ResourceLocation id = ResourceLocation.tryParse(text);
                if (id != null) {
                    ids.add(id);
                }
            }
            cached = new CachedRules(mode, source, new Rules(mode, ids));
            cachedRules = cached;
        }
        return cached.rules();
    }
}
