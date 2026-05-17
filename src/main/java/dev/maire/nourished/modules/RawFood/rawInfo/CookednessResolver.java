package dev.maire.nourished.modules.RawFood.rawInfo;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import dev.maire.nourished.tooling.scanner.ClassificationResult;
import dev.maire.nourished.tooling.scanner.FoodTokenStemmer;
import dev.maire.nourished.tooling.scanner.ScanCache;
import dev.maire.nourished.tooling.scanner.UnassignedFoodScanner;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a cookedness float (0.0 = completely raw, 1.0 = fully cooked) for any food item.
 * Results are cached permanently per item until cache invalidation.
 *
 * <p>This is a read-only layer; all heavy computation happens once and is cached.
 * The hot path (player eating) does nothing but a cache lookup.</p>
 */
@ApiStatus.Internal
public final class CookednessResolver {

    private static final ConcurrentHashMap<ResourceLocation, Float> CACHE = new ConcurrentHashMap<>();

    private CookednessResolver() {}

    /**
     * Resolves cookedness for the given item ID.
     *
     * @param itemId the item's registry ID
     * @return cookedness value between 0.0 (raw) and 1.0 (cooked)
     */
    public static float resolve(ResourceLocation itemId) {
        Float cached = CACHE.get(itemId);
        if (cached != null) {
            return cached;
        }

        ScanCache scanCache = UnassignedFoodScanner.getCache();
        if (scanCache == null) {
            CACHE.put(itemId, 1.0f);
            return 1.0f;
        }

        ClassificationResult result = scanCache.get(itemId);
        if (result == null) {
            CACHE.put(itemId, 1.0f);
            return 1.0f;
        }

        float cookedness = computeCookedness(itemId, result);
        CACHE.put(itemId, cookedness);
        return cookedness;
    }

    /**
     * Clears the cookedness cache.
     * Called from /nourished invalidatecache alongside UnassignedFoodScanner.invalidateCache().
     */
    public static void invalidate() {
        CACHE.clear();
    }

    /**
     * Returns the cached cookedness value without computing.
     * Used for debug/tooltip display.
     *
     * @param itemId the item's registry ID
     * @return the cached cookedness, or empty if not cached
     */
    public static Optional<Float> peek(ResourceLocation itemId) {
        return Optional.ofNullable(CACHE.get(itemId));
    }

    private static float computeCookedness(ResourceLocation itemId, ClassificationResult result) {
        List<String> tokens = FoodTokenStemmer.tokenizeForScoring(itemId.getPath());

        Float baseCookedness = null;
        if (RawFoodConfig.hasAnyToken(RawSeverity.SEVERE, tokens)) {
            baseCookedness = 0.0f;
        } else if (RawFoodConfig.hasAnyToken(RawSeverity.MEDIUM, tokens)) {
            baseCookedness = 0.25f;
        } else if (RawFoodConfig.hasAnyToken(RawSeverity.MILD, tokens)) {
            baseCookedness = 0.6f;
        }

        if (baseCookedness == null) {
            return 1.0f;
        }

        float confidence = result.confidenceScore();
        float cookedness = baseCookedness + ((1.0f - baseCookedness) * (1.0f - confidence));
        return Math.max(0.0f, Math.min(1.0f, cookedness));
    }
}
