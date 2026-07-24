package dev.maire.nourished.modules.RawFood.rawInfo;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.marie.framework.scanner.RecipeInheritanceResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the nutrient bars that a raw food item would have provided had it been cooked first.
 * Results are cached permanently per item until cache invalidation.
 *
 * <p>This is a read-only layer; all heavy computation happens once and is cached.
 * The hot path (player eating) does nothing but a cache lookup.</p>
 */
@ApiStatus.Internal
public final class CookedVersionResolver {

    private static final ConcurrentHashMap<ResourceLocation, Optional<Map<String, Float>>> CACHE
            = new ConcurrentHashMap<>();

    private CookedVersionResolver() {}

    /**
     * Resolves the nutrient bars for the cooked version of the given raw item.
     *
     * @param rawItemId the raw item's registry ID
     * @return nutrient bars for the cooked version, or empty if no cooked version exists
     */
    public static Optional<Map<String, Float>> resolveCooked(ResourceLocation rawItemId) {
        Optional<Map<String, Float>> cached = CACHE.get(rawItemId);
        if (cached != null) {
            return cached;
        }

        RecipeManager recipeManager = FoodNutritionRegistry.getServerRecipeManager();
        if (recipeManager == null) {
            CACHE.put(rawItemId, Optional.empty());
            return Optional.empty();
        }

        RecipeInheritanceResolver recipeResolver = new RecipeInheritanceResolver(recipeManager);
        ResourceLocation cookedItemId = recipeResolver.findCookedOutput(rawItemId);

        if (cookedItemId == null) {
            Nourished.LOGGER.debug("[CookedVersionResolver] {} → no cooked version found", rawItemId);
            CACHE.put(rawItemId, Optional.empty());
            return Optional.empty();
        }

        Item cookedItem = BuiltInRegistries.ITEM.get(cookedItemId);
        if (cookedItem == null) {
            Nourished.LOGGER.debug("[CookedVersionResolver] {} → no cooked version found", rawItemId);
            CACHE.put(rawItemId, Optional.empty());
            return Optional.empty();
        }

        ItemStack cookedStack = new ItemStack(cookedItem);
        Map<String, Float> cookedNutrients = NutrientClassificationLookup.resolveBars(cookedItem);

        if (cookedNutrients.isEmpty()) {
            Nourished.LOGGER.debug("[CookedVersionResolver] {} → no cooked version found", rawItemId);
            CACHE.put(rawItemId, Optional.empty());
            return Optional.empty();
        }

        Nourished.LOGGER.debug("[CookedVersionResolver] {} → {} (nutrients={})", rawItemId, cookedItemId, cookedNutrients);
        Optional<Map<String, Float>> result = Optional.of(Map.copyOf(cookedNutrients));
        CACHE.put(rawItemId, result);
        return result;
    }

    /**
     * Clears the cooked version cache.
     * Called from /nourished invalidatecache.
     */
    public static void invalidate() {
        CACHE.clear();
    }

    /**
     * Returns the cached cooked nutrient bars without computing.
     * Used for debug output.
     *
     * @param itemId the item's registry ID
     * @return the cached cooked nutrients, or empty if not cached
     */
    public static Optional<Map<String, Float>> peek(ResourceLocation itemId) {
        return CACHE.getOrDefault(itemId, Optional.empty());
    }
}
