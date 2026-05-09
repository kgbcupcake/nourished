package dev.maire.nourished.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.maire.nourished.api.CompatDefinition;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Auto-detects loaded food mods that have not been registered in the compat registry.
 */
public final class AutoCompatDiscovery {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> VANILLA_MOD_IDS = Set.of("minecraft", "neoforge", "nourished", "java", "forge");
    private static final float DEFAULT_HINT_AMOUNT = 0.1f;

    private AutoCompatDiscovery() {}

    public static void discover() {
        Nourished.LOGGER.info("[Nourished] AutoCompatDiscovery.discover() called — evaluating {} mods", ModList.get().getMods().size());
        try {
            Path autoCompatDir = FMLPaths.CONFIGDIR.get().resolve("nourished/auto_compat");
            Files.createDirectories(autoCompatDir);
            Set<String> registeredModIds = new HashSet<>(ModCompat.getDetected().keySet());

            for (var modInfo : ModList.get().getMods()) {
                String modId = modInfo.getModId();
                Nourished.LOGGER.info("[Nourished] AutoCompat checking: {}", modId);
                if (modId != null && modId.toLowerCase(Locale.ROOT).contains("mama")) {
                    Nourished.LOGGER.info("[Nourished] Loaded mod id containing 'mama': {}", modId);
                }
                if (VANILLA_MOD_IDS.contains(modId)) {
                    Nourished.LOGGER.info("[Nourished] AutoCompat skipping {} — reason: {}", modId, "vanilla/system mod");
                    continue;
                }
                if (registeredModIds.contains(modId)) {
                    Nourished.LOGGER.info("[Nourished] AutoCompat skipping {} — reason: {}", modId, "already registered");
                    continue;
                }

                Map<ResourceLocation, String> hintedMappings = new LinkedHashMap<>();
                List<Item> foodItems = collectFoodItems(modId, hintedMappings);
                Nourished.LOGGER.info("[Nourished] AutoCompat food item count for {}: {}", modId, foodItems.size());
                if (foodItems.isEmpty()) {
                    Nourished.LOGGER.info("[Nourished] AutoCompat skipping {} — reason: {}", modId, "no food items found");
                    continue;
                }

                CompatDefinition definition = CompatDefinition.builder(modId)
                        .category(CompatDefinition.CompatCategory.FOOD_MOD)
                        .addAllFoodTagMappings(Map.of())
                        .build();
                ModCompat.registerExternal(definition);
                registeredModIds.add(modId);

                Nourished.LOGGER.info(
                        "[Nourished] Auto-detected food mod: {} ({} food items found)",
                        modId,
                        foodItems.size()
                );

                writeStubIfMissing(autoCompatDir, modId, foodItems.size(), hintedMappings);
            }
        } catch (Exception e) {
            Nourished.LOGGER.warn("[Nourished] Auto compat discovery failed: {}", e.getMessage());
        }
    }

    private static List<Item> collectFoodItems(String modId, Map<ResourceLocation, String> hintedMappings) {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null || !modId.equals(itemId.getNamespace())) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            FoodProperties foodProperties = item.getFoodProperties(stack, null);
            if (foodProperties == null) {
                continue;
            }

            items.add(item);
            String nutrientKey = resolveMatchedNutrientKey(stack);
            if (nutrientKey != null) {
                hintedMappings.put(itemId, nutrientKey);
            }
        }
        return items;
    }

    private static String resolveMatchedNutrientKey(ItemStack stack) {
        var holder = stack.getItemHolder();
        for (NutrientRegistry.NutrientDef nutrient : NutrientRegistry.getAll()) {
            for (String tagStr : nutrient.tags()) {
                ResourceLocation tagId = ResourceLocation.tryParse(tagStr);
                if (tagId == null) {
                    continue;
                }
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                if (holder.is(tagKey)) {
                    return nutrient.key();
                }
            }
        }
        return null;
    }

    private static void writeStubIfMissing(
            Path autoCompatDir,
            String modId,
            int foodItemCount,
            Map<ResourceLocation, String> hintedMappings
    ) {
        Path stubPath = autoCompatDir.resolve(modId + ".json");
        if (Files.exists(stubPath)) {
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("nourished_schema_version", 1);
        root.addProperty("mod_id", modId);
        root.addProperty("category", "FOOD_MOD");
        root.addProperty("auto_detected", true);
        root.addProperty("food_item_count", foodItemCount);

        JsonObject mappings = new JsonObject();
        for (Map.Entry<ResourceLocation, String> entry : hintedMappings.entrySet()) {
            JsonObject mapping = new JsonObject();
            mapping.addProperty("nutrient", entry.getValue());
            mapping.addProperty("amount", DEFAULT_HINT_AMOUNT);
            mappings.add(entry.getKey().toString(), mapping);
        }
        root.add("mappings", mappings);

        try (Writer writer = Files.newBufferedWriter(stubPath)) {
            GSON.toJson(root, writer);
            Nourished.LOGGER.info("[Nourished] Wrote auto-compat stub: {}", stubPath);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[Nourished] Failed to write auto-compat stub {}: {}", stubPath, e.getMessage());
        }
    }
}
