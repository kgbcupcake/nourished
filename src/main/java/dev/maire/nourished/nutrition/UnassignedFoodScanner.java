package dev.maire.nourished.nutrition;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.nutrition.scanner.ClassificationResult;
import dev.maire.nourished.nutrition.scanner.FoodClassifier;
import dev.maire.nourished.nutrition.scanner.RecipeInheritanceResolver;
import dev.maire.nourished.nutrition.scanner.ScanCache;
import dev.maire.nourished.nutrition.scanner.ScanReportWriter;
import dev.maire.nourished.nutrition.scanner.TagRecommendationWriter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Production-grade heuristic food classification engine for NeoForge 1.21.1.
 *
 * <p>Pipeline stages:</p>
 * <ol>
 *   <li>Registry Scan - Find all items with FoodProperties</li>
 *   <li>Signal Analysis - Multi-signal, weighted classification</li>
 *   <li>Confidence Validation - Spread-based, not threshold-based</li>
 *   <li>Tag Recommendation - Auto-generate JSON entries for high-confidence hits</li>
 *   <li>Report Output - Human-readable .txt AND machine-readable .json</li>
 * </ol>
 *
 * <p>This is a developer-facing tool, not player-facing. Zero impact on normal gameplay.</p>
 */
public final class UnassignedFoodScanner {

    private static final float DEFAULT_SPREAD_THRESHOLD = 3.0f;
    private static final boolean DEFAULT_ENABLE_RECIPE_INHERITANCE = true;

    private static volatile ScanCache cache;
    private static volatile boolean initialized = false;

    /**
     * Represents a single scan hit for an unassigned food item.
     *
     * @param itemId The item's registry ID
     * @param fallbackNutrient The fallback nutrient category assigned
     * @param result The full classification result (nullable for backward compat)
     */
    public record ScanHit(
            ResourceLocation itemId,
            String fallbackNutrient,
            @Nullable ClassificationResult result
    ) {
        public ScanHit(ResourceLocation itemId, String fallbackNutrient) {
            this(itemId, fallbackNutrient, null);
        }
    }

    /**
     * Scan options for customizing behavior.
     */
    public record ScanOptions(
            boolean enableRecipeInheritance,
            float confidenceSpreadThreshold,
            boolean writeReports,
            boolean writeRecommendations,
            @Nullable RecipeManager recipeManager,
            @Nullable Consumer<String> progressCallback
    ) {
        public static ScanOptions defaults() {
            return new ScanOptions(
                    DEFAULT_ENABLE_RECIPE_INHERITANCE,
                    DEFAULT_SPREAD_THRESHOLD,
                    true,
                    true,
                    null,
                    null
            );
        }

        public ScanOptions withRecipeManager(RecipeManager rm) {
            return new ScanOptions(enableRecipeInheritance, confidenceSpreadThreshold,
                    writeReports, writeRecommendations, rm, progressCallback);
        }

        public ScanOptions withProgressCallback(Consumer<String> callback) {
            return new ScanOptions(enableRecipeInheritance, confidenceSpreadThreshold,
                    writeReports, writeRecommendations, recipeManager, callback);
        }

        public ScanOptions withThreshold(float threshold) {
            return new ScanOptions(enableRecipeInheritance, threshold,
                    writeReports, writeRecommendations, recipeManager, progressCallback);
        }

        public ScanOptions withRecipeInheritance(boolean enabled) {
            return new ScanOptions(enabled, confidenceSpreadThreshold,
                    writeReports, writeRecommendations, recipeManager, progressCallback);
        }

        public ScanOptions withReports(boolean enabled) {
            return new ScanOptions(enableRecipeInheritance, confidenceSpreadThreshold,
                    enabled, writeRecommendations, recipeManager, progressCallback);
        }

        public ScanOptions withRecommendations(boolean enabled) {
            return new ScanOptions(enableRecipeInheritance, confidenceSpreadThreshold,
                    writeReports, enabled, recipeManager, progressCallback);
        }
    }

    /**
     * Full scan result including all classification data.
     */
    public record ScanResult(
            List<ScanHit> hits,
            List<ClassificationResult> allResults,
            ScanCache.ScanSummary summary,
            @Nullable ScanCache.ScanDiff diff
    ) {}

    private UnassignedFoodScanner() {}

    // ─────────────────────────────────────────────────────────────────────────────
    // Public API - Backward Compatible
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Simple scan for unassigned foods (backward compatible).
     * Returns items that have no nourished:nutrients/* tag.
     */
    public static List<ScanHit> scan() {
        ScanResult result = scanFull(ScanOptions.defaults().withReports(false).withRecommendations(false));
        return result.hits();
    }

    /**
     * Check if an item stack has any nutrient tag.
     */
    public static boolean hasNutrientTag(ItemStack stack) {
        var holder = stack.getItemHolder();
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagStr));
                if (holder.is(tagKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Extended API
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Run a full scan with custom options, asynchronously.
     */
    public static CompletableFuture<ScanResult> scanAsync(ScanOptions options) {
        return CompletableFuture.supplyAsync(() -> scanFull(options));
    }

    /**
     * Run a full scan with custom options, synchronously.
     */
    public static ScanResult scanFull(ScanOptions options) {
        ensureInitialized();

        Consumer<String> progress = options.progressCallback() != null
                ? options.progressCallback()
                : msg -> {};

        progress.accept("Starting food scan...");

        List<String> nutrientKeys = NutrientRegistry.getKeys();
        String fallbackKey = nutrientKeys.isEmpty() ? "grains" : nutrientKeys.get(0);

        RecipeInheritanceResolver recipeResolver = null;
        if (options.enableRecipeInheritance() && options.recipeManager() != null) {
            recipeResolver = new RecipeInheritanceResolver(options.recipeManager());
        }

        FoodClassifier classifier = new FoodClassifier(
                nutrientKeys,
                options.enableRecipeInheritance() && recipeResolver != null,
                options.confidenceSpreadThreshold(),
                recipeResolver
        );

        progress.accept("Scanning item registry...");

        List<Item> foodItems = new ArrayList<>();
        int alreadyTagged = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            FoodProperties food = item.components().get(DataComponents.FOOD);
            if (food == null || food.nutrition() == 0) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            if (hasNutrientTag(stack)) {
                alreadyTagged++;
                continue;
            }

            foodItems.add(item);
        }

        progress.accept("Found " + foodItems.size() + " untagged food items...");

        Map<ResourceLocation, ClassificationResult> classifiedResults = new ConcurrentHashMap<>();
        Map<String, Map<String, Float>> namespaceAverages = new HashMap<>();

        progress.accept("Running classification pass 1 (without namespace peers)...");
        for (Item item : foodItems) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) continue;

            ClassificationResult cached = cache.get(itemId);
            if (cached != null) {
                classifiedResults.put(itemId, cached);
                continue;
            }

            ClassificationResult result = classifier.classify(
                    item,
                    classifiedResults::get,
                    Map.of()
            );
            classifiedResults.put(itemId, result);
            cache.put(itemId, result);
        }

        progress.accept("Computing namespace averages...");
        namespaceAverages = FoodClassifier.computeNamespaceAverages(classifiedResults);

        progress.accept("Running classification pass 2 (with namespace peers)...");
        for (Item item : foodItems) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) continue;

            ClassificationResult result = classifier.classify(
                    item,
                    classifiedResults::get,
                    namespaceAverages
            );
            classifiedResults.put(itemId, result);
            cache.put(itemId, result);
        }

        List<ClassificationResult> allResults = new ArrayList<>(classifiedResults.values());
        List<ScanHit> hits = new ArrayList<>();

        int autoClassified = 0;
        int uncertain = 0;

        ConcurrentHashMap<ResourceLocation, String> dominantCategories = new ConcurrentHashMap<>();

        for (ClassificationResult r : allResults) {
            String dominant = r.dominant() != null ? r.dominant() : fallbackKey;
            dominantCategories.put(r.itemId(), dominant);

            if (r.uncertain()) {
                uncertain++;
            } else {
                autoClassified++;
            }

            hits.add(new ScanHit(r.itemId(), dominant, r));
        }

        ScanCache.ScanSummary summary = new ScanCache.ScanSummary(
                cache.getModListHash(),
                System.currentTimeMillis(),
                foodItems.size(),
                autoClassified,
                uncertain,
                alreadyTagged,
                dominantCategories
        );

        ScanCache.ScanDiff diff = summary.diffFrom(cache.getLastSummary());
        cache.setLastSummary(summary);

        progress.accept("Scan complete: " + autoClassified + " classified, " + uncertain + " uncertain");

        if (options.writeReports()) {
            progress.accept("Writing reports...");
            try {
                ScanReportWriter.writeReports(allResults, summary, diff);
            } catch (IOException e) {
                Nourished.LOGGER.error("[UnassignedFoodScanner] Failed to write reports", e);
            }
        }

        if (options.writeRecommendations()) {
            progress.accept("Writing tag recommendations...");
            try {
                TagRecommendationWriter.writeRecommendations(allResults, options.confidenceSpreadThreshold());
            } catch (IOException e) {
                Nourished.LOGGER.error("[UnassignedFoodScanner] Failed to write recommendations", e);
            }
        }

        progress.accept("Done.");

        return new ScanResult(hits, allResults, summary, diff);
    }

    /**
     * Get the scan cache. May be null if not initialized.
     */
    @Nullable
    public static ScanCache getCache() {
        ensureInitialized();
        return cache;
    }

    /**
     * Invalidate the cache, forcing a full rescan next time.
     */
    public static void invalidateCache() {
        if (cache != null) {
            cache.invalidate();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────────

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            cache = new ScanCache();
            initialized = true;
        }
    }
}
