package dev.maire.nourished.core.nutrition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.config.NourishedConfig;

import net.minecraft.core.component.DataComponents;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Food nutrient values and diet-bar classification are driven only by datapack item tags under
 * {@code data/nourished/tags/item/nutrients/} (see {@code nourished:nutrients/*}).
 */
@ApiStatus.Internal
public class FoodNutritionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> WARNED_ITEMS = new HashSet<>();

    private static final Map<ResourceLocation, Map<String, Float>> EXTERNAL_CLASSIFICATIONS = new HashMap<>();

    /**
     * Registers an API-driven food classification mapping a food item to a nutrient key with an amount.
     * Called by {@link dev.maire.nourished.api.NourishedAPI#registerFoodClassification}.
     */
    public static void registerClassification(ResourceLocation foodId, String nutrientKey, float amount) {
        EXTERNAL_CLASSIFICATIONS.computeIfAbsent(foodId, k -> new LinkedHashMap<>()).put(nutrientKey, amount);
        LOGGER.info("[FoodNutritionRegistry] Registered external classification: {} -> {} ({})", foodId, nutrientKey, amount);
    }

    /**
     * Returns externally registered classifications for a food item, or null if none.
     */
    public static Map<String, Float> getExternalClassification(ResourceLocation foodId) {
        return EXTERNAL_CLASSIFICATIONS.get(foodId);
    }

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    /** Diet UI deltas; nutrient values are driven by NutrientRegistry keys. */
    public record DietDelta(float calories, Map<String, Float> nutrients) {}

    /**
     * Called after {@link NutrientRegistry#load()} (and on reload). Classification uses only datapack tags;
     * nothing is rebuilt here.
     */
    public static void init() {
        // Intentionally empty — kept for API compatibility with {@link NutrientRegistry#reload()}.
    }

    /**
     * Resolves all matching diet nutrient keys for an item stack using {@code nourished:nutrients/*} tags.
     * Iterates over NutrientRegistry.getAll() dynamically.
     * If none match, defaults to first registered nutrient.
     *
     * @param warnIfUnmatched when true, logs a WARN for modpack authors when defaulting
     * @return map of nutrient bar key -> match weight
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched) {
        Item item = stack.getItem();
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = stack.getItemHolder();

        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                var tagKey = NourishedRegistryUtils.itemTagKey(tagStr);
                if (holder.is(tagKey)) {
                    matches.put(def.key(), 1.0f);
                    break;
                }
            }
        }

        matches.entrySet().removeIf(entry -> {
            String namespace = itemId.getNamespace();
            String modid = ModCompat.namespaceToModid(namespace);
            return modid != null && !NourishedConfig.get().isTagCompatEnabled(modid);
        });

        if (!matches.isEmpty()) {
            return matches;
        }

        if (warnIfUnmatched) {
            String id = item.getDescriptionId();
            if (WARNED_ITEMS.add(id)) {
                LOGGER.warn(
                        "Nourished: no nutrient tag for {} — attempting name-based guess. Add it to data/nourished/tags/item/nutrients/*.json for accurate classification.",
                        id);
            }
        }

        List<String> keys = NutrientRegistry.getKeys();
        String defaultKey = keys.stream().findFirst().orElse("");
        if (defaultKey.isEmpty()) {
            return matches;
        }
        matches.put(defaultKey, 1.0f);
        return matches;
    }

    /**
     * @param silent when {@code true}, skips {@link #resolvePrimaryNutrientBar} unmatched-item warnings (use on client
     *               tooltips / JEI where tags may not be committed yet).
     */
    public static NutrientValues getNutrients(ItemStack stack, Level level, boolean silent) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        String bar = resolvePrimaryNutrientBar(stack, !silent);
        float totalPoints = Math.max(1.0f, food.nutrition() + Math.max(0f, food.saturation()) * 0.5f);
        return nutrientValuesForBar(bar, totalPoints);
    }

    /** Same as {@link #getNutrients(ItemStack, Level, boolean)} with {@code silent == false}. */
    public static NutrientValues getNutrients(ItemStack stack, Level level) {
        return getNutrients(stack, level, false);
    }

    /** NutrientValues with primary macro weighting for the given diet bar, driven by FoodValueRegistry. */
    private static NutrientValues nutrientValuesForBar(String barKey, float pts) {
        return FoodValueRegistry.getValuesForCategory(barKey, pts);
    }

    public static DietDelta computeDietDelta(
            ItemStack stack,
            Level level,
            int foodNutrition,
            float foodSaturation,
            Map<String, Float> matchedBars) {
        Objects.requireNonNull(level, "level");
        int calories = Math.max(0, Math.round(foodNutrition * 25f));
        Objects.requireNonNull(matchedBars, "matchedBars");

        float matchedWeightTotal = 0f;
        for (float weight : matchedBars.values()) {
            matchedWeightTotal += Math.max(0f, weight);
        }
        String defaultKey = NutrientRegistry.getKeys().stream().findFirst().orElse("");
        if (defaultKey.isEmpty()) {
            return new DietDelta(calories, Map.of());
        }
        if (matchedWeightTotal <= 0f) {
            matchedBars = Map.of(defaultKey, 1.0f);
            matchedWeightTotal = 1.0f;
        }

        float burst = foodNutrition * 0.008f + foodSaturation * 0.010f + 0.004f;
        float totalBurst = burst * matchedWeightTotal;

        Map<String, Float> nutrients = new HashMap<>();
        List<String> keys = NutrientRegistry.getKeys();

        for (String key : keys) {
            nutrients.put(key, 0f);
        }

        for (Map.Entry<String, Float> entry : matchedBars.entrySet()) {
            float weight = Math.max(0f, entry.getValue());
            if (weight <= 0f) {
                continue;
            }
            float contribution = totalBurst * (weight / matchedWeightTotal);
            String key = entry.getKey();
            if (nutrients.containsKey(key)) {
                nutrients.put(key, nutrients.get(key) + contribution);
            }
        }

        float scale = configuredNutrientGainScale();
        Map<String, Float> scaledNutrients = new HashMap<>();
        for (Map.Entry<String, Float> e : nutrients.entrySet()) {
            scaledNutrients.put(e.getKey(), e.getValue() * scale);
        }

        float perBiteMax = configuredNutrientGainPerBiteMax();
        for (String k : scaledNutrients.keySet()) {
            scaledNutrients.put(k, Math.min(scaledNutrients.get(k), perBiteMax));
        }

        return new DietDelta(calories, scaledNutrients);
    }

    public static DietDelta computeDietDelta(ItemStack stack, Level level, int foodNutrition, float foodSaturation) {
        return computeDietDelta(
                stack,
                level,
                foodNutrition,
                foodSaturation,
                resolveNutrientBars(stack, false));
    }

    private static String resolvePrimaryNutrientBar(ItemStack stack, boolean warnIfUnmatched) {
        Map<String, Float> bars = resolveNutrientBars(stack, warnIfUnmatched);
        if (!bars.isEmpty()) return bars.keySet().iterator().next();
        return NutrientRegistry.getKeys().stream().findFirst().orElse("");
    }

    private static float configuredNutrientGainScale() {
        try {
            return Mth.clamp((float) NourishedConfig.get().nutrientGainScale(), 0.5f, 20f);
        } catch (IllegalStateException ignored) {
            return 5f;
        }
    }

    private static float configuredNutrientGainPerBiteMax() {
        try {
            return Mth.clamp((float) NourishedConfig.get().nutrientGainPerBiteMax(), 0.05f, 1f);
        } catch (IllegalStateException ignored) {
            return 0.2f;
        }
    }
}
