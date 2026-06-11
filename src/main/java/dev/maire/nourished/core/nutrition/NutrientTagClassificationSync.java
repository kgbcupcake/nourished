package dev.maire.nourished.core.nutrition;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.runtime.SourceRegistry;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.maire.nourished.core.Nourished;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * Seeds {@link SourceRegistry} from {@code nourished:nutrients/*} item tags after tags are bound.
 * Datapack tag-based {@code source_classifications} JSON cannot run earlier in reload order.
 */
@ApiStatus.Internal
public final class NutrientTagClassificationSync {

    private NutrientTagClassificationSync() {}

    public static void syncFromNutrientTags() {
        int registered = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = MarieRegistryUtils.itemKey(item);
            if (itemId == null) {
                continue;
            }
            if (SourceRegistry.hasAuthoritativeClassification(itemId)) {
                continue;
            }
            Map<String, Float> tagScores = FoodNutritionRegistry.getNutrientTagScores(item);
            if (tagScores.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Float> entry : tagScores.entrySet()) {
                SourceRegistry.registerClassification(itemId, entry.getKey(), entry.getValue());
                registered++;
            }
        }
        Nourished.LOGGER.info("[NutrientTagClassificationSync] Registered {} tag-derived classifications in SourceRegistry",
                registered);
    }
}
