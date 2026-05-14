package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.cache.BoundedLRU;
import dev.maire.nourished.core.nutrition.cache.RunningAverage;
import dev.maire.nourished.core.nutrition.stages.CommunityTagStage;
import dev.maire.nourished.core.nutrition.stages.HardFallbackStage;
import dev.maire.nourished.core.nutrition.stages.KeywordSuffixStage;
import dev.maire.nourished.core.nutrition.stages.NamespacePeerStage;
import dev.maire.nourished.core.nutrition.stages.RecipeInheritanceStage;
import dev.maire.nourished.core.nutrition.stages.ResolutionStageHandler;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
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

/**
 * Runtime inference pipeline that classifies untagged food items through a 5-stage cascade.
 * Runs after {@code nourished:nutrients/*} tag checks fail. All signal weights are loaded from
 * the scanner spec registry — zero hardcoded nutrient strings.
 *
 * <p>Thread-safe singleton. Server-side recipe stage is gated on a non-null {@link RecipeManager};
 * client callers pass {@code null} to skip it.</p>
 */
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

    /**
     * Attempts to infer nutrient bar weights for an untagged food item through a 5-stage cascade.
     *
     * @param stack         the food item stack (must not be empty)
     * @param recipeManager server recipe manager, or {@code null} to skip recipe inheritance
     * @return inferred nutrient weights, or empty map if the item is not edible / registries are empty
     */
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
        Nourished.LOGGER.debug("[RuntimeFoodResolver] cache miss entering inference pipeline: {}", itemId);

        Holder<Item> holder = stack.getItemHolder();
        Set<String> validKeys = Set.copyOf(nutrientKeys);
        StageContext ctx = new StageContext(holder, itemId, recipeManager, namespacePeers, validKeys);

        ResolutionResult result = null;
        for (ResolutionStageHandler handler : handlers) {
            result = handler.resolve(itemId, ctx);
            if (result != null) break;
        }

        if (result == null) {
            result = new ResolutionResult(Map.of(), 0f, ResolutionStage.HARD_FALLBACK, "pipeline exhausted");
        }

        resolvedCache.put(itemId, result);

        if (result.stage() == ResolutionStage.COMMUNITY_TAG
                || result.stage() == ResolutionStage.KEYWORD_SUFFIX
                || result.stage() == ResolutionStage.RECIPE_INHERITANCE) {
            namespacePeers.computeIfAbsent(itemId.getNamespace(), k -> new RunningAverage())
                    .add(result.nutrients());
        }

        Nourished.LOGGER.debug("[RuntimeFoodResolver] {} resolved via {} (confidence={}): {}",
                itemId, result.stage(), String.format("%.2f", result.confidence()), result.debugReason());

        return result.toNutrientMap();
    }

    public void invalidateCache() {
        int size = resolvedCache.size();
        resolvedCache.clear();
        recipeCache.clear();
        namespacePeers.clear();
        Nourished.LOGGER.info("[RuntimeFoodResolver] Cache invalidated. Was: {} entries", size);
    }

    public CacheStats getCacheStats() {
        return new CacheStats(cacheHits.get(), cacheMisses.get(), resolvedCache.size());
    }
}
