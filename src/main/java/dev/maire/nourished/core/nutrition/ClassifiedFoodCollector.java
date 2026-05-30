package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a snapshot of all foods with known nutrient scores in {@link FoodNutritionRegistry}:
 * datapack tags (authoritative), then API or scanner cache entries.
 */
@ApiStatus.Internal
public final class ClassifiedFoodCollector {

    private ClassifiedFoodCollector() {}

    /**
     * @return item id to per-nutrient scores; empty when nothing is classified
     */
    public static Map<ResourceLocation, Map<String, Float>> collectAllClassifiedFoodScores() {
        Map<ResourceLocation, Map<String, Float>> out = new LinkedHashMap<>();
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
                out.put(itemId, tagScores);
                continue;
            }

            Map<String, Float> external = FoodNutritionRegistry.getExternalClassification(itemId);
            if (external != null && !external.isEmpty()) {
                out.put(itemId, Map.copyOf(external));
            }
        }

        return Map.copyOf(out);
    }
}
