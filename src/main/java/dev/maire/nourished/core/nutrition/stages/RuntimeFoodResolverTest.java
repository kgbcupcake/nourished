package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.tooling.scanner.FoodTokenStemmer;
import dev.maire.nourished.core.nutrition.CacheStats;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;

/**
 * Package-private validation for {@link RuntimeFoodResolver} stage methods, cache stats, and stem map.
 * Can be invoked from a game test or integration test harness.
 */
@ApiStatus.Internal
final class RuntimeFoodResolverTest {

    private RuntimeFoodResolverTest() {}

    /**
     * Runs all validations. Returns a summary string; throws {@link AssertionError} on failure.
     */
    static String runAll() {
        int passed = 0;

        testStemmerSpecExamples();
        passed++;

        testCacheStatsInitiallyZero();
        passed++;

        testStage2EmptyForUnknownTokens();
        passed++;

        testInvalidateCacheResetsStats();
        passed++;

        return "[RuntimeFoodResolverTest] " + passed + " validations passed";
    }

    private static void testStemmerSpecExamples() {
        assertEquals("tomato", FoodTokenStemmer.stem("tomatoes"), "tomatoes -> tomato");
        assertEquals("berry", FoodTokenStemmer.stem("berries"), "berries -> berry");
        assertEquals("meatball", FoodTokenStemmer.stem("meatballs"), "meatballs -> meatball");
    }

    private static void testCacheStatsInitiallyZero() {
        RuntimeFoodResolver resolver = RuntimeFoodResolver.getInstance();
        resolver.invalidateCache();
        CacheStats stats = resolver.getCacheStats();
        if (stats.size() != 0) {
            throw new AssertionError("Cache size should be 0 after invalidation, was " + stats.size());
        }
    }

    private static void testStage2EmptyForUnknownTokens() {
        if (NutrientRegistry.getKeys().isEmpty()) {
            Nourished.LOGGER.debug("[RuntimeFoodResolverTest] Skipping stage2 test — no nutrient keys loaded");
            return;
        }
        ResourceLocation fakeId = ResourceLocation.tryParse("test_ns:zzz_xyzzy_qux");
        if (fakeId == null) {
            throw new AssertionError("Failed to parse test ResourceLocation");
        }
        KeywordSuffixStage stage = new KeywordSuffixStage();
        Map<String, Float> result = stage.score(fakeId);
        if (!result.isEmpty()) {
            throw new AssertionError("Stage 2 should return empty for gibberish tokens, got " + result);
        }
    }

    private static void testInvalidateCacheResetsStats() {
        RuntimeFoodResolver resolver = RuntimeFoodResolver.getInstance();
        resolver.invalidateCache();
        CacheStats stats = resolver.getCacheStats();
        if (stats.size() != 0) {
            throw new AssertionError("Cache should be empty after invalidation, size=" + stats.size());
        }
        if (stats.avgResolveNanos() != 0L || stats.slowestResolveNanos() != 0L || stats.slowestItem() != null
                || stats.recipeTimeouts() != 0) {
            throw new AssertionError("Timing stats should reset after invalidation: " + stats);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
