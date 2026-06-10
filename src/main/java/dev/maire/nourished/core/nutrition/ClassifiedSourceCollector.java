package dev.maire.nourished.core.nutrition;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.scanner.ClassificationResult;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds a snapshot of all foods with known nutrient scores in {@link FoodNutritionRegistry}:
 * datapack tags (authoritative), then API or scanner cache entries.
 */
@ApiStatus.Internal
public final class ClassifiedSourceCollector {

    private ClassifiedSourceCollector() {}

    /**
     * @return full classification results for every classified food; empty when nothing is classified
     */
    public static List<ClassificationResult> collectAllClassifiedSources() {
        List<ClassificationResult> out = new ArrayList<>();
        var excluded = ScannerSpecRegistry.get().excludedItems();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item.components().get(DataComponents.FOOD) == null) {
                continue;
            }
            ResourceLocation itemId = item.builtInRegistryHolder().key().location();
            if (excluded.contains(itemId.toString())) {
                continue;
            }

            Map<String, Float> tagScores = FoodNutritionRegistry.getNutrientTagScores(item);
            if (!tagScores.isEmpty()) {
                out.add(toClassificationResult(itemId, tagScores, true));
                continue;
            }

            Map<String, Float> external = FoodNutritionRegistry.getExternalClassification(itemId);
            if (external != null && !external.isEmpty()) {
                out.add(toClassificationResult(itemId, Map.copyOf(external), false));
            }
        }

        return List.copyOf(out);
    }

    private static ClassificationResult toClassificationResult(
            ResourceLocation itemId,
            Map<String, Float> scores,
            boolean tagClassified
    ) {
        List<Map.Entry<String, Float>> sorted = scores.entrySet().stream()
                .sorted(Comparator
                        .comparing(Map.Entry<String, Float>::getValue, Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .toList();

        if (sorted.isEmpty()) {
            return ClassificationResult.empty(itemId, "");
        }

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
                tagClassified
        );
    }
}
