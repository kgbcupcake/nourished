package dev.maire.nourished.tooling.scanner.stages;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.tooling.scanner.ClassificationResult;
import dev.maire.nourished.tooling.scanner.ClassificationSignal;
import dev.maire.nourished.tooling.scanner.RecipeInheritanceResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Scanner signal stage: inherits nutrient scores from recipe ingredients.
 * Tag-classified targets skip entirely; authoritative tag keys on the target are never overridden.
 */
@ApiStatus.Internal
public final class RecipeInheritanceStage {

    private RecipeInheritanceStage() {}

    /**
     * Resolves recipe inheritance and merges qualifying nutrients into {@code scores}.
     */
    public static void apply(
            Item item,
            ResourceLocation itemId,
            Map<String, Float> scores,
            List<ClassificationSignal> signals,
            boolean tagClassified,
            @Nullable RecipeInheritanceResolver recipeResolver,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            float multiplier
    ) {
        if (tagClassified || recipeResolver == null) {
            return;
        }

        Set<String> authoritativeKeys = new HashSet<>(FoodNutritionRegistry.getNutrientTagScores(item).keySet());
        Function<ResourceLocation, ClassificationResult> lookup = buildLookup(classifiedLookup);

        Map<String, Float> recipeContribs = recipeResolver.resolve(item, lookup);
        if (recipeContribs.isEmpty()) {
            return;
        }

        Map<String, Float> scaled = new HashMap<>();
        for (Map.Entry<String, Float> e : recipeContribs.entrySet()) {
            String key = e.getKey();
            if (authoritativeKeys.contains(key)) {
                continue;
            }
            if (scores.getOrDefault(key, 0f) <= 0f) {
                scores.merge(key, e.getValue() * multiplier, Float::sum);
                scaled.put(key, e.getValue() * multiplier);
            }
        }

        if (!scaled.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_RECIPE_INHERITANCE,
                    "recipe_ingredients",
                    scaled
            ));
        }
    }

    private static Function<ResourceLocation, ClassificationResult> buildLookup(
            Function<ResourceLocation, ClassificationResult> classifiedLookup
    ) {
        return id -> {
            ClassificationResult cached = classifiedLookup.apply(id);
            if (cached != null) {
                return cached;
            }
            Item ingredient = BuiltInRegistries.ITEM.get(id);
            if (ingredient == null) {
                return null;
            }
            Map<String, Float> tagScores = FoodNutritionRegistry.getNutrientTagScores(ingredient);
            if (tagScores.isEmpty()) {
                return null;
            }
            return toTagClassifiedResult(id, tagScores);
        };
    }

    private static ClassificationResult toTagClassifiedResult(ResourceLocation itemId, Map<String, Float> scores) {
        List<Map.Entry<String, Float>> sorted = scores.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .toList();

        String dominant = sorted.get(0).getKey();
        String secondary = sorted.size() > 1 ? sorted.get(1).getKey() : null;
        float topScore = sorted.get(0).getValue();
        float secondScore = sorted.size() > 1 ? sorted.get(1).getValue() : 0f;
        float spread = topScore - secondScore;

        return new ClassificationResult(
                itemId,
                scores,
                dominant,
                secondary,
                spread,
                List.of(),
                false,
                true
        );
    }
}
