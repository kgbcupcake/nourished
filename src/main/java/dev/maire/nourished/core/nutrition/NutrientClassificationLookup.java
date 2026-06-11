package dev.maire.nourished.core.nutrition;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.runtime.SourceRegistry;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Resolves nutrient bar weights through MarieLib's default {@code sourceValueResolver},
 * backed by {@link SourceRegistry}.
 */
@ApiStatus.Internal
public final class NutrientClassificationLookup {

    private NutrientClassificationLookup() {}

    public static Map<String, Float> resolveBars(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return Map.of();
        }
        if (level != null && MarieLibContext.isRegistered()) {
            return MarieLibContext.get().sourceValueResolver().apply(stack, level);
        }
        return resolveBars(stack.getItem());
    }

    public static Map<String, Float> resolveBars(Item item) {
        ResourceLocation itemId = MarieRegistryUtils.itemKey(item);
        if (itemId == null) {
            return Map.of();
        }
        Map<String, Float> external = SourceRegistry.getExternalClassification(itemId);
        if (external != null && !external.isEmpty()) {
            return Map.copyOf(external);
        }
        return FoodNutritionRegistry.getNutrientTagScores(item);
    }
}
