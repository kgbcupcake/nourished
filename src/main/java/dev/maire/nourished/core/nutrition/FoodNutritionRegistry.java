package dev.maire.nourished.core.nutrition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import dev.maire.nourished.tooling.scanner.FoodClassifier;
import dev.maire.nourished.tooling.scanner.RecipeInheritanceResolver;
import dev.maire.nourished.tooling.scanner.RecipeInheritanceResolver.RecipeInheritanceStep;
import dev.maire.nourished.tooling.scanner.ScanCache;
import dev.maire.nourished.tooling.scanner.UnassignedFoodScanner;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    /** Scores at or below this are treated as no signal (unknown classification). */
    private static final float CLASSIFICATION_SCORE_EPS = 1e-5f;

    /** @GuardedBy("itself — ConcurrentHashSet") */
    private static final Set<String> WARNED_ITEMS = ConcurrentHashMap.newKeySet();

    private static final int EXTERNAL_CLASSIFICATION_CAP = 4096;

    /** @GuardedBy("itself — ConcurrentHashMap") */
    private static final Map<ResourceLocation, Map<String, Float>> EXTERNAL_CLASSIFICATIONS = new ConcurrentHashMap<>();

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
     * Resolves diet nutrient bar weights from {@code nourished:nutrients/*} tags, optional scanner/classifier
     * fallback when untagged, and (server-side only) weighted recipe ingredient inheritance when the item has
     * no explicit tag matches. Any tag match is authoritative: classifier, cache, and recipe inheritance are skipped.
     *
     * @param warnIfUnmatched when true, logs a WARN when falling back from no nutrient tags
     * @param level when non-null and not client-side, recipe inheritance may run; client tooltips should pass a
     *              client level or {@code null} so inheritance is skipped
     * @return map of nutrient bar key -> match weight
     */
    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched, @Nullable Level level) {
        Item item = stack.getItem();
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Map<String, Float> tagMatches = collectNutrientTagMatches(item);

        if (!tagMatches.isEmpty()) {
            return tagMatches;
        }

        boolean allowRecipeInheritance = level != null && !level.isClientSide();

        Map<String, Float> result = new LinkedHashMap<>();
        if (warnIfUnmatched) {
            String id = item.getDescriptionId();
            if (WARNED_ITEMS.add(id)) {
                LOGGER.warn(
                        "Nourished: no nutrient tag for {} — attempting name-based guess. Add it to data/nourished/tags/item/nutrients/*.json for accurate classification.",
                        id);
            }
        }
        result.putAll(resolveUntaggedNutrientBars(item, itemId));

        if (allowRecipeInheritance) {
            mergeRecipeInheritedBars(item, result, null);
        }

        return result;
    }

    /**
     * Resolves nutrient bars and captures classification / recipe-inheritance detail for structured debug logs.
     * Does not mutate caches. When {@code level} is client-side, recipe inheritance is skipped (same as
     * {@link #resolveNutrientBars}).
     */
    public static NutrientResolutionDiagnostic resolveNutrientBarsDiagnostic(ItemStack stack, Level level) {
        Item item = stack.getItem();
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
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

        List<String> validKeys = NutrientRegistry.getKeys();
        if (validKeys.isEmpty()) {
            return new NutrientResolutionDiagnostic(
                    Map.of(),
                    "UNCLASSIFIED",
                    List.of("none"),
                    null,
                    List.of(),
                    false
            );
        }
        Set<String> validKeySet = Set.copyOf(validKeys);

        ScanCache cache = UnassignedFoodScanner.getCache();
        ClassificationResult classification = null;
        String path;
        Map<String, Float> result = new LinkedHashMap<>();

        if (cache != null && cache.get(itemId) != null) {
            classification = cache.get(itemId);
            result.putAll(nutrientBarsFromClassification(classification, validKeySet));
            path = "CACHE_HIT";
        } else {
            FoodProperties food = item.components().get(DataComponents.FOOD);
            if (food == null || food.nutrition() <= 0) {
                path = "UNCLASSIFIED";
            } else {
                FoodClassifier classifier = new FoodClassifier(
                        validKeys,
                        false,
                        scannerConfidenceSpreadThreshold(),
                        null
                );
                classification = classifier.classify(item, id -> null, Map.of());
                result.putAll(nutrientBarsFromClassification(classification, validKeySet));
                path = "CLASSIFIER_RUN";
            }
        }

        boolean allowRecipeInheritance = level != null && !level.isClientSide();
        List<RecipeInheritanceStep> recipeTrace = new ArrayList<>();
        if (allowRecipeInheritance) {
            mergeRecipeInheritedBars(item, result, recipeTrace);
        }

        if (result.isEmpty()) {
            path = "UNCLASSIFIED";
        }

        return new NutrientResolutionDiagnostic(
                new LinkedHashMap<>(result),
                path,
                List.of("none"),
                classification,
                recipeTrace,
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
        return resolveNutrientBars(stack, warnIfUnmatched, null);
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

    private static void mergeRecipeInheritedBars(Item item, Map<String, Float> out, @Nullable List<RecipeInheritanceStep> traceOut) {
        RecipeInheritanceResolver resolver = serverRecipeInheritanceResolver;
        if (resolver == null) {
            return;
        }

        Set<String> existingKeys = new HashSet<>(out.keySet());
        Set<String> validNutrientKeys = Set.copyOf(NutrientRegistry.getKeys());
        if (validNutrientKeys.isEmpty()) {
            return;
        }

        Map<String, Float> raw = resolver.resolve(item, FoodNutritionRegistry::lookupClassificationForRecipe, traceOut);
        if (raw.isEmpty()) {
            return;
        }

        Map<String, Float> additions = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : raw.entrySet()) {
            String key = e.getKey();
            if (!validNutrientKeys.contains(key) || existingKeys.contains(key)) {
                continue;
            }
            float v = Math.max(0f, e.getValue());
            if (v > CLASSIFICATION_SCORE_EPS) {
                additions.merge(key, v, Float::sum);
            }
        }

        float sum = 0f;
        for (float v : additions.values()) {
            sum += v;
        }
        if (sum <= CLASSIFICATION_SCORE_EPS) {
            return;
        }

        for (Map.Entry<String, Float> e : additions.entrySet()) {
            out.merge(e.getKey(), e.getValue() / sum, Float::sum);
        }
    }

    @Nullable
    private static ClassificationResult lookupClassificationForRecipe(ResourceLocation itemId) {
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }
        Item ingredient = BuiltInRegistries.ITEM.get(itemId);
        ClassificationResult tagBased = classificationFromNutrientTagsOnly(ingredient, itemId);
        if (tagBased != null) {
            return tagBased;
        }

        ScanCache cache = UnassignedFoodScanner.getCache();
        if (cache != null) {
            ClassificationResult cached = cache.get(itemId);
            if (cached != null) {
                return cached;
            }
        }

        FoodProperties food = ingredient.components().get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) {
            return null;
        }

        List<String> validKeys = NutrientRegistry.getKeys();
        if (validKeys.isEmpty()) {
            return null;
        }

        FoodClassifier classifier = new FoodClassifier(
                validKeys,
                false,
                scannerConfidenceSpreadThreshold(),
                null
        );
        return classifier.classify(ingredient, FoodNutritionRegistry::lookupClassificationForRecipe, Map.of());
    }

    @Nullable
    private static ClassificationResult classificationFromNutrientTagsOnly(Item item, ResourceLocation itemId) {
        Map<String, Float> matches = collectNutrientTagMatches(item);
        if (matches.isEmpty()) {
            return null;
        }
        Map<String, Float> scores = new HashMap<>(matches);
        String dominant = matches.entrySet().stream()
                .max(Map.Entry.<String, Float>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(matches.keySet().iterator().next());
        return new ClassificationResult(
                itemId,
                scores,
                dominant,
                null,
                1f,
                List.of(),
                false
        );
    }

    private static float scannerConfidenceSpreadThreshold() {
        try {
            return (float) NourishedConfig.get().scannerConfidenceSpreadThreshold();
        } catch (IllegalStateException ignored) {
            return 0f;
        }
    }

    /**
     * Tagless resolution: scan cache first, then inline classifier (single pass, no recipe/peers).
     */
    private static Map<String, Float> resolveUntaggedNutrientBars(Item item, ResourceLocation itemId) {
        List<String> validKeys = NutrientRegistry.getKeys();
        if (validKeys.isEmpty()) {
            return Map.of();
        }
        Set<String> validKeySet = Set.copyOf(validKeys);

        ScanCache cache = UnassignedFoodScanner.getCache();
        ClassificationResult cached = cache != null ? cache.get(itemId) : null;
        if (cached != null) {
            return nutrientBarsFromClassification(cached, validKeySet);
        }

        FoodProperties food = item.components().get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) {
            return new LinkedHashMap<>();
        }

        FoodClassifier classifier = new FoodClassifier(
                validKeys,
                false,
                scannerConfidenceSpreadThreshold(),
                null
        );
        ClassificationResult result = classifier.classify(item, id -> null, Map.of());
        return nutrientBarsFromClassification(result, validKeySet);
    }

    /**
     * Maps a classification to bar weights: confident → dominant only; uncertain → normalized positive scores;
     * no usable score mass → empty map.
     */
    private static Map<String, Float> nutrientBarsFromClassification(
            ClassificationResult result,
            Set<String> validNutrientKeys
    ) {
        float maxScore = 0f;
        for (String key : validNutrientKeys) {
            maxScore = Math.max(maxScore, result.scores().getOrDefault(key, 0f));
        }
        if (maxScore <= CLASSIFICATION_SCORE_EPS) {
            return new LinkedHashMap<>();
        }

        if (!result.uncertain()) {
            String dominant = result.dominant();
            if (dominant != null
                    && validNutrientKeys.contains(dominant)
                    && result.scores().getOrDefault(dominant, 0f) > CLASSIFICATION_SCORE_EPS) {
                Map<String, Float> single = new LinkedHashMap<>();
                single.put(dominant, 1.0f);
                return single;
            }
        }

        float noiseFloor = maxScore * 0.02f;
        float sum = 0f;
        for (String key : validNutrientKeys) {
            float s = Math.max(0f, result.scores().getOrDefault(key, 0f));
            if (s >= noiseFloor) {
                sum += s;
            }
        }
        if (sum <= CLASSIFICATION_SCORE_EPS) {
            String dominant = result.dominant();
            if (dominant != null
                    && validNutrientKeys.contains(dominant)
                    && result.scores().getOrDefault(dominant, 0f) > CLASSIFICATION_SCORE_EPS) {
                Map<String, Float> single = new LinkedHashMap<>();
                single.put(dominant, 1.0f);
                return single;
            }
            return new LinkedHashMap<>();
        }

        Map<String, Float> weighted = new LinkedHashMap<>();
        for (String key : validNutrientKeys) {
            float s = Math.max(0f, result.scores().getOrDefault(key, 0f));
            if (s >= noiseFloor) {
                weighted.put(key, s / sum);
            }
        }
        return weighted;
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

    public static DietDelta computeDietDelta(ItemStack stack, Level level, int foodNutrition, float foodSaturation) {
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
