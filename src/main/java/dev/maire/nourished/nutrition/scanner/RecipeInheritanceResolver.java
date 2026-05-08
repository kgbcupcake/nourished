package dev.maire.nourished.nutrition.scanner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves recipe ingredient inheritance for food classification.
 * Server-side only component with strict performance constraints.
 *
 * <p>Constraints per spec:</p>
 * <ul>
 *   <li>Hard limit: depth 2, max 8 ingredients per recipe</li>
 *   <li>Skip recipes with more than 8 ingredients</li>
 *   <li>Cache all recipe lookups in HashMap</li>
 *   <li>Apply 0.5x confidence decay per level</li>
 * </ul>
 */
public final class RecipeInheritanceResolver {

    private static final int MAX_DEPTH = 2;
    private static final int MAX_INGREDIENTS = 8;
    private static final float DECAY_PER_LEVEL = 0.5f;

    private final Map<ResourceLocation, List<ResourceLocation>> recipeCache;
    @Nullable
    private final RecipeManager recipeManager;

    public RecipeInheritanceResolver(@Nullable RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.recipeCache = new HashMap<>();
    }

    /**
     * Resolve recipe inheritance for an item.
     *
     * @param item The item to analyze
     * @param classifiedLookup Function to lookup already-classified items
     * @return Nutrient contributions from recipe ingredients
     */
    public Map<String, Float> resolve(
            Item item,
            Function<ResourceLocation, ClassificationResult> classifiedLookup
    ) {
        if (recipeManager == null) {
            return Map.of();
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return Map.of();
        }

        return resolveRecursive(itemId, classifiedLookup, 0, new HashMap<>());
    }

    private Map<String, Float> resolveRecursive(
            ResourceLocation itemId,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            int depth,
            Map<ResourceLocation, Boolean> visited
    ) {
        if (depth >= MAX_DEPTH) {
            return Map.of();
        }

        if (visited.containsKey(itemId)) {
            return Map.of();
        }
        visited.put(itemId, true);

        List<ResourceLocation> ingredients = getIngredients(itemId);
        if (ingredients.isEmpty()) {
            return Map.of();
        }

        Map<String, Float> contributions = new HashMap<>();
        float decayFactor = (float) Math.pow(DECAY_PER_LEVEL, depth + 1);

        for (ResourceLocation ingredientId : ingredients) {
            ClassificationResult result = classifiedLookup.apply(ingredientId);
            if (result != null && !result.uncertain()) {
                for (Map.Entry<String, Float> e : result.scores().entrySet()) {
                    float contribution = e.getValue() * decayFactor / ingredients.size();
                    contributions.merge(e.getKey(), contribution, Float::sum);
                }
            } else {
                Map<String, Float> inherited = resolveRecursive(ingredientId, classifiedLookup, depth + 1, visited);
                for (Map.Entry<String, Float> e : inherited.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue() / ingredients.size(), Float::sum);
                }
            }
        }

        return contributions;
    }

    private List<ResourceLocation> getIngredients(ResourceLocation itemId) {
        List<ResourceLocation> cached = recipeCache.get(itemId);
        if (cached != null) {
            return cached;
        }

        List<ResourceLocation> ingredients = new ArrayList<>();

        if (recipeManager != null) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null) {
                recipeCache.put(itemId, ingredients);
                return ingredients;
            }

            ItemStack resultStack = new ItemStack(item);

            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                Recipe<?> recipe = holder.value();

                try {
                    ItemStack recipeResult = recipe.getResultItem(null);
                    if (recipeResult == null || !ItemStack.isSameItem(recipeResult, resultStack)) {
                        continue;
                    }

                    List<Ingredient> recipeIngredients = recipe.getIngredients();
                    if (recipeIngredients.size() > MAX_INGREDIENTS) {
                        continue;
                    }

                    for (Ingredient ingredient : recipeIngredients) {
                        ItemStack[] items = ingredient.getItems();
                        if (items.length > 0) {
                            ResourceLocation ingId = BuiltInRegistries.ITEM.getKey(items[0].getItem());
                            if (ingId != null && !ingId.equals(itemId)) {
                                ingredients.add(ingId);
                            }
                        }
                    }

                    if (!ingredients.isEmpty()) {
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (ingredients.size() > MAX_INGREDIENTS) {
            ingredients = ingredients.subList(0, MAX_INGREDIENTS);
        }

        recipeCache.put(itemId, ingredients);
        return ingredients;
    }

    /**
     * Clear the recipe cache. Call when mod list changes.
     */
    public void clearCache() {
        recipeCache.clear();
    }

    /**
     * Get the current cache size for diagnostics.
     */
    public int cacheSize() {
        return recipeCache.size();
    }
}
