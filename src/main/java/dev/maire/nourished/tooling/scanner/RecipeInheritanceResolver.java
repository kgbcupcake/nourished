package dev.maire.nourished.tooling.scanner;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.nutrition.MultiNutrientInheritance;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import dev.maire.nourished.tooling.classification.ClassificationTraceStep;
import dev.maire.nourished.tooling.classification.TraceStepId;
import dev.maire.nourished.tooling.classification.TraceStepStatus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
@ApiStatus.Internal
public final class RecipeInheritanceResolver {

    /**
     * One ingredient lookup during recipe inheritance (for debug / telemetry).
     *
     * @param ingredientId    ingredient item id
     * @param depth           recursion depth (0 = direct recipe ingredients)
     * @param decayFactor     {@code pow(0.5, depth + 1)} applied to this hop
     * @param nutrientContributions per-nutrient weighted contribution from this hop (after decay and ingredient count split)
     */
    public record RecipeInheritanceStep(
            ResourceLocation ingredientId,
            int depth,
            float decayFactor,
            Map<String, Float> nutrientContributions
    ) {
        public RecipeInheritanceStep {
            nutrientContributions = Map.copyOf(nutrientContributions);
        }
    }

    private static final int MAX_DEPTH = 2;
    private static final int MAX_INGREDIENTS = 8;
    private static final float DECAY_PER_LEVEL = 0.5f;

    private final Map<ResourceLocation, List<ResourceLocation>> recipeCache;
    @Nullable
    private final RecipeManager recipeManager;

    public RecipeInheritanceResolver(@Nullable RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.recipeCache = new ConcurrentHashMap<>();
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
        return resolve(item, classifiedLookup, null);
    }

    /**
     * Same as {@link #resolve(Item, Function)}; when {@code traceOut} is non-null, appends a
     * {@link RecipeInheritanceStep} for each ingredient branch that resolves from a confident classification.
     */
    public Map<String, Float> resolve(
            Item item,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            @Nullable List<RecipeInheritanceStep> traceOut
    ) {
        return resolve(item, classifiedLookup, traceOut, null);
    }

    /**
     * Same as above but also emits ClassificationTraceStep for trace diagnostics.
     */
    public Map<String, Float> resolve(
            Item item,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            @Nullable List<RecipeInheritanceStep> traceOut,
            @Nullable List<ClassificationTraceStep> classificationTraceOut
    ) {
        if (recipeManager == null) {
            return Map.of();
        }

        ResourceLocation itemId = NourishedRegistryUtils.itemKey(item);
        if (itemId == null) {
            return Map.of();
        }

        List<ResourceLocation> ingredients = getIngredients(itemId);

        if (classificationTraceOut != null) {
            Map<String, Object> recipeDetail = new LinkedHashMap<>();
            recipeDetail.put("recipeFound", !ingredients.isEmpty());
            recipeDetail.put("ingredientCount", ingredients.size());
            recipeDetail.put("timeout", false);
            classificationTraceOut.add(new ClassificationTraceStep(
                    TraceStepId.RECIPE_LOOKUP,
                    ingredients.isEmpty() ? TraceStepStatus.FAILURE : TraceStepStatus.SUCCESS,
                    ingredients.isEmpty() ? "No recipe found" : "Found recipe with " + ingredients.size() + " ingredient(s)",
                    recipeDetail));
        }

        Map<String, Float> aggregated = resolveRecursive(itemId, classifiedLookup, 0, new HashMap<>(), traceOut, classificationTraceOut);
        return MultiNutrientInheritance.filterQualifyingNutrients(aggregated, MultiNutrientInheritance.threshold());
    }

    private Map<String, Float> resolveRecursive(
            ResourceLocation itemId,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            int depth,
            Map<ResourceLocation, Boolean> visited,
            @Nullable List<RecipeInheritanceStep> traceOut
    ) {
        return resolveRecursive(itemId, classifiedLookup, depth, visited, traceOut, null);
    }

    private Map<String, Float> resolveRecursive(
            ResourceLocation itemId,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            int depth,
            Map<ResourceLocation, Boolean> visited,
            @Nullable List<RecipeInheritanceStep> traceOut,
            @Nullable List<ClassificationTraceStep> classificationTraceOut
    ) {
        if (depth >= MAX_DEPTH) {
            return Map.of();
        }

        if (visited.containsKey(itemId)) {
            if (classificationTraceOut != null) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("ingredientId", itemId.toString());
                detail.put("source", "NONE");
                detail.put("skipped", true);
                detail.put("skipReason", "CYCLE_DETECTED");
                classificationTraceOut.add(new ClassificationTraceStep(
                        TraceStepId.INGREDIENT_RESOLUTION,
                        TraceStepStatus.SKIPPED,
                        "Ingredient skipped: cycle detected",
                        detail));
            }
            return Map.of();
        }
        visited.put(itemId, true);

        List<ResourceLocation> ingredients = getIngredients(itemId);
        if (ingredients.isEmpty()) {
            return Map.of();
        }

        Map<String, Float> contributions = new HashMap<>();
        float decayFactor = (float) Math.pow(DECAY_PER_LEVEL, depth + 1);
        int n = ingredients.size();

        for (ResourceLocation ingredientId : ingredients) {
            ClassificationResult result = classifiedLookup.apply(ingredientId);
            if (result != null && !result.uncertain()) {
                Map<String, Float> stepContribs = new HashMap<>();
                for (Map.Entry<String, Float> e : result.scores().entrySet()) {
                    float contribution = e.getValue() * decayFactor / n;
                    contributions.merge(e.getKey(), contribution, Float::sum);
                    stepContribs.put(e.getKey(), contribution);
                }
                if (traceOut != null) {
                    traceOut.add(new RecipeInheritanceStep(ingredientId, depth, decayFactor, stepContribs));
                }
                if (classificationTraceOut != null) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("ingredientId", ingredientId.toString());
                    detail.put("source", result.tagClassified() ? "TAG" : "SCANNER");
                    detail.put("nutrients", new LinkedHashMap<>(stepContribs));
                    if (result.uncertain()) {
                        detail.put("uncertain", true);
                    }
                    classificationTraceOut.add(new ClassificationTraceStep(
                            TraceStepId.INGREDIENT_RESOLUTION,
                            TraceStepStatus.SUCCESS,
                            "Ingredient classified via " + (result.tagClassified() ? "TAG" : "SCANNER"),
                            detail));
                }
            } else {
                if (classificationTraceOut != null && result == null) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("ingredientId", ingredientId.toString());
                    detail.put("source", "NONE");
                    detail.put("warningCode", "INGREDIENT_UNCLASSIFIED");
                    classificationTraceOut.add(new ClassificationTraceStep(
                            TraceStepId.INGREDIENT_RESOLUTION,
                            TraceStepStatus.FAILURE,
                            "Ingredient unclassified",
                            detail));
                } else if (classificationTraceOut != null && result != null && result.uncertain()) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("ingredientId", ingredientId.toString());
                    detail.put("source", "SCANNER");
                    detail.put("uncertain", true);
                    detail.put("warningCode", "INGREDIENT_UNCLASSIFIED");
                    classificationTraceOut.add(new ClassificationTraceStep(
                            TraceStepId.INGREDIENT_RESOLUTION,
                            TraceStepStatus.WARNING,
                            "Ingredient classification uncertain",
                            detail));
                }
                Map<String, Float> inherited = resolveRecursive(ingredientId, classifiedLookup, depth + 1, visited, traceOut, classificationTraceOut);
                for (Map.Entry<String, Float> e : inherited.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue() / n, Float::sum);
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
                            ResourceLocation ingId = NourishedRegistryUtils.itemKey(items[0]);
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
     * Finds what the given raw item cooks into by checking furnace, smoker, and campfire recipes.
     * Returns the first cooked output found, or {@code null} if no cooking recipe exists.
     *
     * @param rawItemId the raw item's registry ID
     * @return the cooked output item ID, or {@code null} if no cooking recipe found
     */
    @Nullable
    public ResourceLocation findCookedOutput(ResourceLocation rawItemId) {
        if (recipeManager == null) {
            return null;
        }

        Item rawItem = BuiltInRegistries.ITEM.get(rawItemId);
        if (rawItem == null) {
            return null;
        }

        ItemStack rawStack = new ItemStack(rawItem);

        // Check smelting recipes (furnace)
        for (RecipeHolder<SmeltingRecipe> holder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            try {
                SmeltingRecipe recipe = holder.value();
                if (recipe.getIngredients().stream().anyMatch(ing -> ing.test(rawStack))) {
                    ItemStack result = recipe.getResultItem(null);
                    if (result != null && !result.isEmpty()) {
                        return NourishedRegistryUtils.itemKey(result);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Check smoking recipes
        for (RecipeHolder<SmokingRecipe> holder : recipeManager.getAllRecipesFor(RecipeType.SMOKING)) {
            try {
                SmokingRecipe recipe = holder.value();
                if (recipe.getIngredients().stream().anyMatch(ing -> ing.test(rawStack))) {
                    ItemStack result = recipe.getResultItem(null);
                    if (result != null && !result.isEmpty()) {
                        return NourishedRegistryUtils.itemKey(result);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Check campfire recipes
        for (RecipeHolder<CampfireCookingRecipe> holder : recipeManager.getAllRecipesFor(RecipeType.CAMPFIRE_COOKING)) {
            try {
                CampfireCookingRecipe recipe = holder.value();
                if (recipe.getIngredients().stream().anyMatch(ing -> ing.test(rawStack))) {
                    ItemStack result = recipe.getResultItem(null);
                    if (result != null && !result.isEmpty()) {
                        return NourishedRegistryUtils.itemKey(result);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
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
