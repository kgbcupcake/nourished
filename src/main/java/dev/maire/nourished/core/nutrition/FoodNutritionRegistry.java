package dev.maire.nourished.core.nutrition;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.compat.ModCompat;
import dev.marie.framework.curve.math.CurveGrid;
import dev.marie.framework.scanner.RecipeInheritanceResolver;
import dev.marie.framework.util.MarieRegistryUtils;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

/**
 * Food-specific helpers retained after classification migration to
 * {@link dev.marie.framework.runtime.SourceRegistry}.
 */
@ApiStatus.Internal
public final class FoodNutritionRegistry {

    private static final float NUTRITION_CEIL = 10f;
    private static final float SATURATION_CEIL = 20f;

    private static final Map<String, TagKey<Item>> TAG_KEY_CACHE = new ConcurrentHashMap<>();

    /**
     * Milk buckets consume like food for effects but omit {@link DataComponents#FOOD}. Used for nutrient math everywhere
     * (consumption pipeline, HUD tooltips, JEI helper) so tag-based dairy gains match.
     */
    public static final FoodProperties MILK_BUCKET_FOOD_PROPERTIES = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0f)
            .alwaysEdible()
            .build();

    @Nullable
    private static volatile RecipeManager serverRecipeManager;

    @Nullable
    private static volatile RecipeInheritanceResolver serverRecipeInheritanceResolver;

    private FoodNutritionRegistry() {}

    /**
     * {@link FoodProperties} used when applying or previewing nourishment for an item stack. Vanilla milk buckets
     * participate in tagging but never report food properties.
     *
     * @param entity contextual entity for modded hooks; nullable on client previews
     */
    @Nullable
    public static FoodProperties foodPropertiesForNutrition(ItemStack stack, @Nullable LivingEntity entity) {
        FoodProperties base = stack.getItem().getFoodProperties(stack, entity);
        if (base != null) {
            return base;
        }
        if (stack.is(Items.MILK_BUCKET)) {
            return MILK_BUCKET_FOOD_PROPERTIES;
        }
        if (!getNutrientTagScores(stack.getItem()).isEmpty()) {
            return MILK_BUCKET_FOOD_PROPERTIES;
        }
        return null;
    }

    /**
     * Binds the active server {@link RecipeManager} for recipe-based diet bar inheritance.
     * Called from server lifecycle and after datapack reload; pass {@code null} on server stop.
     */
    public static void bindServerRecipeManager(@Nullable RecipeManager recipeManager) {
        serverRecipeManager = recipeManager;
        serverRecipeInheritanceResolver = recipeManager != null ? new RecipeInheritanceResolver(recipeManager) : null;
        if (recipeManager != null) {
            RuntimeFoodResolver.getInstance().buildRecipeIndex(recipeManager);
        }
    }

    /**
     * Returns the bound server recipe manager, or {@code null} if the server is not ready.
     */
    @Nullable
    public static RecipeManager getServerRecipeManager() {
        return serverRecipeManager;
    }

    /**
     * Called after {@link NutrientRegistry#load()} (and on reload). Kept for API compatibility.
     */
    public static void init() {
        // Classifications now live in SourceRegistry via datapack and MarieAPI.
    }

    public static void clearTagKeyCache() {
        TAG_KEY_CACHE.clear();
    }

    /**
     * Nutrient bar weights from datapack {@code nourished:nutrients/*} tags (compat-filtered).
     * Used by tooling that snapshots tag-classified foods.
     */
    public static Map<String, Float> getNutrientTagScores(Item item) {
        Map<String, Float> matches = collectNutrientTagMatches(item);
        return matches.isEmpty() ? Map.of() : Map.copyOf(matches);
    }

    private static Map<String, Float> collectNutrientTagMatches(Item item) {
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = item.builtInRegistryHolder();

        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                var tagKey = TAG_KEY_CACHE.computeIfAbsent(tagStr, MarieRegistryUtils::itemTagKey);
                if (holder.is(tagKey)) {
                    matches.put(def.key(), NutrientWeightRegistry.getWeights(itemId).getOrDefault(def.key(), 1.0f));
                    break;
                }
            }
        }

        matches.entrySet().removeIf(entry -> {
            String namespace = itemId.getNamespace();
            String modid = ModCompat.namespaceToModid(namespace);
            return modid != null && !NourishedConfig.get().isTagCompatEnabled(modid);
        });

        return matches;
    }

    public record DietDelta(int calories, Map<String, Float> nutrients) {}

    public static DietDelta computeDietDelta(
            ItemStack stack,
            @Nullable Level level,
            int foodNutrition,
            float foodSaturation,
            Map<String, Float> matchedBars) {
        int calories = Math.max(0, Math.round(foodNutrition * 25f));
        Objects.requireNonNull(matchedBars, "matchedBars");

        float matchedWeightTotal = 0f;
        for (float weight : matchedBars.values()) {
            matchedWeightTotal += Math.max(0f, weight);
        }
        if (matchedWeightTotal <= 0f) {
            Map<String, Float> zeros = new HashMap<>();
            for (String key : NutrientRegistry.getKeys()) {
                zeros.put(key, 0f);
            }
            return new DietDelta(calories, zeros);
        }

        float burst = foodNutrition * 0.008f + foodSaturation * 0.010f + 0.004f;
        float totalBurst = burst * matchedWeightTotal;

        Map<String, Float> nutrients = new HashMap<>();
        for (String key : NutrientRegistry.getKeys()) {
            nutrients.put(key, 0f);
        }

        for (Map.Entry<String, Float> entry : matchedBars.entrySet()) {
            float weight = Math.max(0f, entry.getValue());
            if (weight <= 0f) {
                continue;
            }
            float contribution = totalBurst * (weight / matchedWeightTotal);
            String key = entry.getKey();
            nutrients.merge(key, contribution, Float::sum);
        }

        Map<String, Float> scaledNutrients = new HashMap<>();
        if (NourishedConfig.get().enableNutrientCurves()) {
            String globalDefaultPreset = NourishedConfig.get().defaultCurvePreset();
            float intensity = computeNormalizedIntensity(foodNutrition, foodSaturation);
            for (Map.Entry<String, Float> entry : matchedBars.entrySet()) {
                float weight = Math.max(0f, entry.getValue());
                if (weight <= 0f) {
                    continue;
                }
                String key = entry.getKey();
                float confidence = weight / matchedWeightTotal;
                CurveGrid grid = NutrientCurveRegistry.resolve(key, globalDefaultPreset);
                float multiplier = grid.evaluate(intensity, confidence);
                float baseValue = nutrients.getOrDefault(key, 0f);
                scaledNutrients.put(key, baseValue * multiplier);
            }
            for (String key : NutrientRegistry.getKeys()) {
                scaledNutrients.putIfAbsent(key, 0f);
            }
        } else {
            float scale = configuredNutrientGainScale();
            for (Map.Entry<String, Float> e : nutrients.entrySet()) {
                scaledNutrients.put(e.getKey(), e.getValue() * scale);
            }
        }

        float perBiteMax = configuredNutrientGainPerBiteMax();
        for (String k : scaledNutrients.keySet()) {
            scaledNutrients.put(k, Math.min(scaledNutrients.get(k), perBiteMax));
        }

        return new DietDelta(calories, scaledNutrients);
    }

    private static float computeNormalizedIntensity(int foodNutrition, float foodSaturation) {
        return (foodNutrition * foodSaturation) / (NUTRITION_CEIL * SATURATION_CEIL);
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
