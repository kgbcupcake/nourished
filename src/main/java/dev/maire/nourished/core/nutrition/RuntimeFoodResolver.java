package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diagnostics.NourishedUnknownFoodLogger;
import dev.maire.nourished.core.nutrition.cache.BoundedLRU;
import dev.maire.nourished.core.nutrition.cache.RunningAverage;
import dev.maire.nourished.core.nutrition.stages.CommunityTagStage;
import dev.maire.nourished.core.nutrition.stages.HardFallbackStage;
import dev.maire.nourished.core.nutrition.stages.KeywordSuffixStage;
import dev.maire.nourished.core.nutrition.stages.NamespacePeerStage;
import dev.maire.nourished.core.nutrition.stages.RecipeInheritanceStage;
import dev.maire.nourished.core.nutrition.stages.ResolutionStageHandler;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@ApiStatus.Internal
public final class RuntimeFoodResolver {

    private static final RuntimeFoodResolver INSTANCE = new RuntimeFoodResolver();

    public static RuntimeFoodResolver getInstance() {
        return INSTANCE;
    }

    private final BoundedLRU<ResourceLocation, ResolutionResult> resolvedCache = new BoundedLRU<>();
    private final BoundedLRU<ResourceLocation, List<ResourceLocation>> recipeCache = new BoundedLRU<>();
    private final ConcurrentHashMap<String, RunningAverage> namespacePeers = new ConcurrentHashMap<>();
    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger cacheMisses = new AtomicInteger();
    private final AtomicLong totalResolveNanos = new AtomicLong(0);
    private final AtomicLong slowestResolveNanos = new AtomicLong(0);
    private final AtomicReference<ResourceLocation> slowestItem = new AtomicReference<>(null);
    private final AtomicInteger recipeTimeouts = new AtomicInteger(0);

    private final List<ResolutionStageHandler> handlers;

    private RuntimeFoodResolver() {
        handlers = List.of(
                new CommunityTagStage(),
                new KeywordSuffixStage(),
                new RecipeInheritanceStage(recipeCache),
                new NamespacePeerStage(),
                new HardFallbackStage()
        );
    }

    public Map<String, Float> resolve(ItemStack stack, @Nullable RecipeManager recipeManager) {
        if (stack.isEmpty() || stack.getItem() == null) return Map.of();

        List<String> nutrientKeys = NutrientRegistry.getKeys();
        if (nutrientKeys.isEmpty()) return Map.of();

        Item item = stack.getItem();
        ResourceLocation itemId = NourishedRegistryUtils.itemKey(item);
        if (itemId == null) return Map.of();

        FoodProperties food = item.components().get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) return Map.of();

        ResolutionResult cached = resolvedCache.get(itemId);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached.toNutrientMap();
        }

        cacheMisses.incrementAndGet();
        return resolveUncached(stack, itemId, recipeManager).toNutrientMap();
    }

    public @Nullable ResolutionResult resolveWithResult(ItemStack stack, @Nullable RecipeManager recipeManager) {
        if (stack.isEmpty() || stack.getItem() == null) return null;

        List<String> nutrientKeys = NutrientRegistry.getKeys();
        if (nutrientKeys.isEmpty()) return null;

        Item item = stack.getItem();
        ResourceLocation itemId = NourishedRegistryUtils.itemKey(item);
        if (itemId == null) return null;

        FoodProperties food = item.components().get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) return null;

        ResolutionResult cached = resolvedCache.get(itemId);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached.withCacheHit(true);
        }

        cacheMisses.incrementAndGet();
        return resolveUncached(stack, itemId, recipeManager);
    }

    private ResolutionResult resolveUncached(ItemStack stack, ResourceLocation itemId, @Nullable RecipeManager recipeManager) {
        if (ScannerSpecRegistry.get().excludedItems().contains(itemId.toString())) {
            return new ResolutionResult(
                    Map.of(), Map.of(), List.of(), Map.of(), Map.of(),
                    false, 0f, ResolutionStage.HARD_FALLBACK, "excluded_items");
        }
        long start = System.nanoTime();

        Nourished.LOGGER.debug("[RuntimeFoodResolver] cache miss entering inference pipeline: {}", itemId);

        List<String> nutrientKeys = NutrientRegistry.getKeys();
        Holder<Item> holder = stack.getItemHolder();
        Set<String> validKeys = Set.copyOf(nutrientKeys);
        StageContext ctx = new StageContext(holder, itemId, recipeManager, namespacePeers, validKeys);

        ResolutionResult result = null;
        for (ResolutionStageHandler handler : handlers) {
            result = handler.resolve(itemId, ctx);
            if (result != null) break;
        }

        if (result == null) {
            result = new ResolutionResult(
                    Map.of(), Map.of(), List.of(), Map.of(), Map.of(),
                    false, 0f, ResolutionStage.HARD_FALLBACK, "pipeline exhausted");
        }

        resolvedCache.put(itemId, result);
        NourishedUnknownFoodLogger.log(result, itemId);

        if (result.stage() == ResolutionStage.COMMUNITY_TAG
                || result.stage() == ResolutionStage.KEYWORD_SUFFIX
                || result.stage() == ResolutionStage.COMPOSITE
                || result.stage() == ResolutionStage.RECIPE_INHERITANCE) {
            namespacePeers.computeIfAbsent(itemId.getNamespace(), k -> new RunningAverage())
                    .add(result.nutrients());
        }

        Nourished.LOGGER.debug("[RuntimeFoodResolver] {} resolved via {} (confidence={}): {}",
                itemId, result.stage(), String.format("%.2f", result.confidence()), result.debugReason());

        long elapsed = System.nanoTime() - start;
        recordTiming(elapsed, itemId);

        return result;
    }

    private void recordTiming(long elapsedNanos, ResourceLocation itemId) {
        totalResolveNanos.addAndGet(elapsedNanos);
        for (; ; ) {
            long prev = slowestResolveNanos.get();
            if (elapsedNanos < prev) {
                break;
            }
            if (slowestResolveNanos.compareAndSet(prev, elapsedNanos)) {
                slowestItem.set(itemId);
                break;
            }
        }
    }

    public static void recordRecipeTimeout() {
        getInstance().incrementRecipeTimeoutsInternal();
    }

    void incrementRecipeTimeoutsInternal() {
        recipeTimeouts.incrementAndGet();
    }

    public void invalidateCache() {
        int size = resolvedCache.size();
        resolvedCache.clear();
        recipeCache.clear();
        namespacePeers.clear();
        totalResolveNanos.set(0);
        slowestResolveNanos.set(0);
        slowestItem.set(null);
        recipeTimeouts.set(0);
        Nourished.LOGGER.info("[RuntimeFoodResolver] Cache invalidated. Was: {} entries", size);
    }

    public CacheStats getCacheStats() {
        int total = cacheMisses.get();
        long avg = total == 0 ? 0L : totalResolveNanos.get() / total;
        return new CacheStats(
                cacheHits.get(),
                cacheMisses.get(),
                resolvedCache.size(),
                avg,
                slowestResolveNanos.get(),
                slowestItem.get(),
                recipeTimeouts.get()
        );
    }
}
