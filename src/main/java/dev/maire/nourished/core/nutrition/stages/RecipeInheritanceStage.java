package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.MultiNutrientInheritance;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.RuntimeCascadeStage;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.nutrition.StageContext;
import dev.maire.nourished.core.nutrition.cache.BoundedLRU;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 3: inherits nutrient classification from confirmed (tag-based) recipe ingredients.
 * Requires at least 2 confirmed ingredient classifications. All recipe-manager access
 * is budget-capped at 5 ms and wrapped in {@code catch (Exception)}.
 */
public final class RecipeInheritanceStage implements ResolutionStageHandler {

    private static final long TIMEOUT_NANOS = 5_000_000L;

    private static final Set<ResourceLocation> RECIPE_SKIP = Set.of(
            ResourceLocation.withDefaultNamespace("air"),
            ResourceLocation.withDefaultNamespace("barrier")
    );

    private final BoundedLRU<ResourceLocation, List<ResourceLocation>> recipeCache;

    public RecipeInheritanceStage(BoundedLRU<ResourceLocation, List<ResourceLocation>> recipeCache) {
        this.recipeCache = recipeCache;
    }

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        RecipeManager recipeManager = ctx.recipeManager();
        if (recipeManager == null) return null;

        long start = System.nanoTime();

        try {
            List<ResourceLocation> ingredients = recipeCache.get(itemId);
            if (ingredients == null) {
                ingredients = discoverRecipeIngredients(itemId, recipeManager, start);
                if (ingredients == null) return null;
                recipeCache.put(itemId, ingredients);
            }

            if (ingredients.isEmpty()) return null;

            Map<String, Float> totalContribs = new HashMap<>();
            int confirmed = 0;

            for (ResourceLocation ingredientId : ingredients) {
                if (System.nanoTime() - start > TIMEOUT_NANOS) {
                    RuntimeFoodResolver.recordRecipeTimeout();
                    Nourished.LOGGER.warn("[RuntimeFoodResolver] Recipe timeout for {}, aborting ({}ns)",
                            itemId, System.nanoTime() - start);
                    return null;
                }

                if (shouldSkipRecipeIngredient(ingredientId)) continue;

                Item ingredientItem = BuiltInRegistries.ITEM.get(ingredientId);
                if (ingredientItem == null) continue;

                Map<String, Float> tagMatches = collectConfirmedNutrientTags(ingredientItem, ingredientId);
                if (!tagMatches.isEmpty()) {
                    for (Map.Entry<String, Float> e : tagMatches.entrySet()) {
                        totalContribs.merge(e.getKey(), e.getValue(), Float::sum);
                    }
                    confirmed++;
                }
            }

            if (confirmed < 2) return null;

            Map<String, Float> averaged = new HashMap<>();
            for (Map.Entry<String, Float> e : totalContribs.entrySet()) {
                averaged.put(e.getKey(), e.getValue() / confirmed);
            }

            Map<String, Float> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Float> e : averaged.entrySet()) {
                if (ctx.validKeys().contains(e.getKey()) && e.getValue() > 0f) {
                    filtered.put(e.getKey(), e.getValue());
                }
            }
            if (filtered.isEmpty()) return null;

            Map<String, Float> qualifying = MultiNutrientInheritance.filterQualifyingNutrients(
                    filtered, MultiNutrientInheritance.threshold());
            if (qualifying.isEmpty()) return null;

            Map<String, String> rejectedSignals = new LinkedHashMap<>();
            for (String key : ctx.validKeys()) {
                if (!qualifying.containsKey(key)) {
                    if (filtered.containsKey(key)) {
                        rejectedSignals.put(key, ResolutionResult.REJECT_LOW_CONFIDENCE);
                    } else {
                        rejectedSignals.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
                    }
                }
            }

            float totalScore = 0f;
            for (float v : filtered.values()) totalScore += v;
            return new ResolutionResult(
                    qualifying, Map.copyOf(averaged),
                    List.of(), Map.of(), rejectedSignals,
                    false, totalScore, RuntimeCascadeStage.RECIPE_INHERITANCE,
                    "recipe ingredient inheritance");

        } catch (Exception e) {
            Nourished.LOGGER.warn("[RuntimeFoodResolver] Recipe exception for {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    @Nullable
    private List<ResourceLocation> discoverRecipeIngredients(
            ResourceLocation itemId, RecipeManager recipeManager, long startNanos
    ) {
        Item targetItem = BuiltInRegistries.ITEM.get(itemId);
        if (targetItem == null) return List.of();
        ItemStack targetStack = new ItemStack(targetItem);

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (System.nanoTime() - startNanos > TIMEOUT_NANOS) {
                RuntimeFoodResolver.recordRecipeTimeout();
                Nourished.LOGGER.warn("[RuntimeFoodResolver] Recipe timeout for {}, aborting ({}ns)",
                        itemId, System.nanoTime() - startNanos);
                return null;
            }

            var recipe = holder.value();
            if (!(recipe instanceof CraftingRecipe) && !(recipe instanceof SmeltingRecipe)) continue;

            try {
                ItemStack recipeResult = recipe.getResultItem(null);
                if (recipeResult == null || !ItemStack.isSameItem(recipeResult, targetStack)) continue;

                List<Ingredient> recipeIngredients = recipe.getIngredients();
                Set<ResourceLocation> uniqueIds = new HashSet<>();

                for (Ingredient ingredient : recipeIngredients) {
                    ItemStack[] items = ingredient.getItems();
                    if (items.length > 0) {
                        ResourceLocation ingId = NourishedRegistryUtils.itemKey(items[0]);
                        if (ingId != null && !ingId.equals(itemId)) {
                            uniqueIds.add(ingId);
                        }
                    }
                }

                if (uniqueIds.size() > 6) continue;
                if (!uniqueIds.isEmpty()) {
                    return new ArrayList<>(uniqueIds);
                }
            } catch (Exception ignored) {
                // malformed recipe — skip
            }
        }

        return List.of();
    }

    private static boolean shouldSkipRecipeIngredient(ResourceLocation id) {
        if (RECIPE_SKIP.contains(id)) return true;
        String path = id.getPath();
        if (path.contains("debug") || path.contains("test") || path.contains("internal")) return true;
        if ("minecraft".equals(id.getNamespace())) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null) {
                FoodProperties food = item.components().get(DataComponents.FOOD);
                if (food == null) return true;
            }
        }
        return false;
    }

    /**
     * Confirmed (tag-based) nutrient classifications only — no inference.
     * Mirrors {@link dev.maire.nourished.core.nutrition.FoodNutritionRegistry#collectNutrientTagMatches}
     * logic including compat toggles.
     */
    private static Map<String, Float> collectConfirmedNutrientTags(Item item, ResourceLocation itemId) {
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
}
