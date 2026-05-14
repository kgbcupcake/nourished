package dev.maire.nourished.core.nutrition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.tooling.scanner.ClassificationResult;
import dev.maire.nourished.tooling.scanner.RecipeInheritanceResolver;
import dev.maire.nourished.tooling.scanner.RecipeInheritanceResolver.RecipeInheritanceStep;

import net.minecraft.core.component.DataComponents;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Food nutrient values and diet-bar classification primarily use datapack item tags under
 * {@code data/nourished/tags/item/nutrients/} (see {@code nourished:nutrients/*}). Items without
 * matching tags may resolve bars via {@link #resolveNutrientBars} (scanner cache and classifier).
 */
@ApiStatus.Internal
public class FoodNutritionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Milk buckets consume like food for effects but omit {@link DataComponents#FOOD}. Used for nutrient math everywhere
     * (consumption pipeline, HUD tooltips, JEI helper) so tag-based dairy gains match.
     */
    public static final FoodProperties MILK_BUCKET_FOOD_PROPERTIES = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0f)
            .alwaysEdible()
            .build();

    /** @GuardedBy("itself — ConcurrentHashSet") */
    private static final Set<String> WARNED_ITEMS = ConcurrentHashMap.newKeySet();

    private static final int EXTERNAL_CLASSIFICATION_CAP = 4096;

    /** @GuardedBy("itself — ConcurrentHashMap") */
    private static final Map<ResourceLocation, Map<String, Float>> EXTERNAL_CLASSIFICATIONS = new ConcurrentHashMap<>();

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
        return null;
    }

    @Nullable
    private static volatile RecipeInheritanceResolver serverRecipeInheritanceResolver;

    /**
     * Binds the active server {@link RecipeManager} for recipe-based diet bar inheritance.
     * Called from server lifecycle and after datapack reload; pass {@code null} on server stop.
     */
    public static void bindServerRecipeManager(@Nullable RecipeManager recipeManager) {
        serverRecipeInheritanceResolver = recipeManager != null ? new RecipeInheritanceResolver(recipeManager) : null;
    }

    /**
     * Registers an API-driven food classification mapping a food item to a nutrient key with an amount.
     * Called by {@link dev.maire.nourished.api.NourishedAPI#registerFoodClassification}.
     */
    public static void registerClassification(ResourceLocation foodId, String nutrientKey, float amount) {
        if (EXTERNAL_CLASSIFICATIONS.size() >= EXTERNAL_CLASSIFICATION_CAP && !EXTERNAL_CLASSIFICATIONS.containsKey(foodId)) {
            LOGGER.warn("[FoodNutritionRegistry] External classification cap ({}) reached — ignoring: {} -> {}",
                    EXTERNAL_CLASSIFICATION_CAP, foodId, nutrientKey);
            return;
        }
        EXTERNAL_CLASSIFICATIONS.computeIfAbsent(foodId, k -> new ConcurrentHashMap<>()).put(nutrientKey, amount);
        LOGGER.info("[FoodNutritionRegistry] Registered external classification: {} -> {} ({})", foodId, nutrientKey, amount);
    }

    /** Clears API-registered external classifications. Called during reload pipeline. */
    public static void clearExternalClassifications() {
        EXTERNAL_CLASSIFICATIONS.clear();
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
     * Full resolution trace for debug logging (server-side recipe inheritance when {@code level} is a server level).
     */
    public record NutrientResolutionDiagnostic(
            Map<String, Float> matchedBars,
            String classifierPath,
            List<String> matchedNutrientTags,
            @Nullable ClassificationResult classification,
            List<RecipeInheritanceStep> recipeInheritance,
            boolean foodOverride
    ) {
        public NutrientResolutionDiagnostic {
            matchedBars = Map.copyOf(matchedBars);
            matchedNutrientTags = List.copyOf(matchedNutrientTags);
            recipeInheritance = List.copyOf(recipeInheritance);
        }
    }

    /**
     * Called after {@link NutrientRegistry#load()} (and on reload). No registry rebuild here;
     * tagless bar resolution uses the scanner cache and classifier at query time in {@link #resolveNutrientBars}.
     */
    public static void init() {
        // Intentionally empty — kept for API compatibility with {@link NutrientRegistry#reload()}.
    }

    /**
     * Resolves diet nutrient bar weights from {@code nourished:nutrients/*} tags, with runtime inference
     * fallback via {@link RuntimeFoodResolver} when untagged. Delegates to
     * {@link #resolveNutrientBars(ItemStack, boolean, RecipeManager)} after extracting the recipe manager
     * from the level (server-side only).
     *
     * @param warnIfUnmatched when true, logs a WARN when falling back from no nutrient tags
     * @param level when non-null and not client-side, recipe inheritance may run; client tooltips should pass a
     *              client level or {@code null} so inheritance is skipped
     * @return map of nutrient bar key -> match weight
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched, @Nullable Level level) {
        RecipeManager rm = null;
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            rm = level.getServer().getRecipeManager();
        }
        return resolveNutrientBars(stack, warnIfUnmatched, rm);
    }

    /**
     * Core resolution: checks {@code nourished:nutrients/*} tags first, then falls back to
     * {@link RuntimeFoodResolver} for untagged food items.
     *
     * @param warnIfUnmatched when true, logs a WARN when falling back from no nutrient tags
     * @param recipeManager   server recipe manager for recipe inheritance, or {@code null} to skip it
     * @return map of nutrient bar key -> match weight (never null)
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched, @Nullable RecipeManager recipeManager) {
        Item item = stack.getItem();
        Map<String, Float> tagMatches = collectNutrientTagMatches(item);

        if (!tagMatches.isEmpty()) {
            return tagMatches;
        }

        if (warnIfUnmatched) {
            String id = item.getDescriptionId();
            if (WARNED_ITEMS.add(id)) {
                LOGGER.warn(
                        "Nourished: no nutrient tag for {} — attempting name-based guess. Add it to data/nourished/tags/item/nutrients/*.json for accurate classification.",
                        id);
            }
        }

        Map<String, Float> inferred = RuntimeFoodResolver.getInstance().resolve(stack, recipeManager);
        if (!inferred.isEmpty()) {
            return inferred;
        }

        return Map.of();
    }

    /**
     * Resolves nutrient bars and captures diagnostic detail for structured debug logs.
     * Uses the same {@link RuntimeFoodResolver} pipeline as the production path so diagnostics match gameplay.
     */
    public static NutrientResolutionDiagnostic resolveNutrientBarsDiagnostic(ItemStack stack, Level level) {
        Item item = stack.getItem();
        Map<String, Float> tagMatches = collectNutrientTagMatches(item);
        List<String> matchedTagIds = collectExactMatchedNutrientTagIds(item, tagMatches);

        if (!tagMatches.isEmpty()) {
            return new NutrientResolutionDiagnostic(
                    new LinkedHashMap<>(tagMatches),
                    "TAG_HIT",
                    matchedTagIds,
                    null,
                    List.of(),
                    false
            );
        }

        RecipeManager rm = null;
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            rm = level.getServer().getRecipeManager();
        }

        Map<String, Float> inferred = RuntimeFoodResolver.getInstance().resolve(stack, rm);
        String path = inferred.isEmpty() ? "UNCLASSIFIED" : "RUNTIME_RESOLVER";

        return new NutrientResolutionDiagnostic(
                new LinkedHashMap<>(inferred),
                path,
                List.of("none"),
                null,
                List.of(),
                false
        );
    }

    /**
     * For each matched nutrient bar key, the first {@code nourished:nutrients/...} tag string the item actually holds.
     */
    private static List<String> collectExactMatchedNutrientTagIds(Item item, Map<String, Float> tagMatches) {
        if (tagMatches.isEmpty()) {
            return List.of();
        }
        var holder = new ItemStack(item).getItemHolder();
        List<String> out = new ArrayList<>();
        for (String barKey : tagMatches.keySet()) {
            for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
                if (!def.key().equals(barKey)) {
                    continue;
                }
                for (String tagStr : def.tags()) {
                    var tagKey = NourishedRegistryUtils.itemTagKey(tagStr);
                    if (holder.is(tagKey)) {
                        out.add(tagKey.location().toString());
                        break;
                    }
                }
                break;
            }
        }
        return out;
    }

    /**
     * Same as {@link #resolveNutrientBars(ItemStack, boolean, Level)} with no recipe inheritance
     * (no {@link Level} context).
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched) {
        return resolveNutrientBars(stack, warnIfUnmatched, (RecipeManager) null);
    }

    private static Map<String, Float> collectNutrientTagMatches(Item item) {
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Map<String, Float> matches = new LinkedHashMap<>();
        var holder = new ItemStack(item).getItemHolder();

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

        return matches;
    }

    /**
     * @param silent when {@code true}, skips {@link #resolvePrimaryNutrientBar} unmatched-item warnings (use on client
     *               tooltips / JEI where tags may not be committed yet).
     */
    public static NutrientValues getNutrients(ItemStack stack, Level level, boolean silent) {
        FoodProperties food = foodPropertiesForNutrition(stack, null);
        if (food == null) {
            return new NutrientValues(0.2f, 0.2f, 0.2f, 0.2f, 0.2f);
        }

        String bar = resolvePrimaryNutrientBar(stack, !silent, level);
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

    public static DietDelta computeDietDelta(ItemStack stack, @Nullable Level level, int foodNutrition, float foodSaturation) {
        return computeDietDelta(
                stack,
                level,
                foodNutrition,
                foodSaturation,
                resolveNutrientBars(stack, false, level));
    }

    private static String resolvePrimaryNutrientBar(ItemStack stack, boolean warnIfUnmatched, Level level) {
        Map<String, Float> bars = resolveNutrientBars(stack, warnIfUnmatched, level);
        if (!bars.isEmpty()) return bars.keySet().iterator().next();
        return "";
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
