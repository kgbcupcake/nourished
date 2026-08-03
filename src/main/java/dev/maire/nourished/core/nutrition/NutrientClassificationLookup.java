package dev.maire.nourished.core.nutrition;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.runtime.RuntimeResolver;
import dev.marie.framework.runtime.SourceRegistry;
import dev.marie.framework.scanner.ExcludedItemsRegistry;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import dev.marie.framework.util.MarieRegistryUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves nutrient bar weights from datapack tags and the runtime classifier cascade.
 */
@ApiStatus.Internal
public final class NutrientClassificationLookup {

    private NutrientClassificationLookup() {}

    public static Map<String, Float> resolveBars(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return Map.of();
        }
        return resolveNutrientBars(stack, false, level);
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

    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched, @Nullable Level level) {
        RecipeManager rm = null;
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            rm = level.getServer().getRecipeManager();
        }
        if (rm == null) {
            rm = FoodNutritionRegistry.getServerRecipeManager();
        }
        return resolveNutrientBars(stack, warnIfUnmatched, rm);
    }

    public static Map<String, Float> resolveNutrientBars(ItemStack stack, boolean warnIfUnmatched) {
        return resolveNutrientBars(stack, warnIfUnmatched, (RecipeManager) null);
    }

    public static Map<String, Float> resolveNutrientBars(
            ItemStack stack, boolean warnIfUnmatched, @Nullable RecipeManager recipeManager) {
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack.getItem());

        Optional<FoodOverrideRegistry.FoodOverride> override = itemId != null
                ? FoodOverrideRegistry.getOverride(itemId.toString())
                : Optional.empty();

        if (itemId != null
                && (ScannerSpecRegistry.get().excludedItems().contains(itemId.toString())
                        || ExcludedItemsRegistry.isExcluded(itemId.toString()))) {
            return override.isPresent() ? Map.copyOf(override.get().nutrients()) : Map.of();
        }

        Map<String, Float> tagMatches = FoodNutritionRegistry.getNutrientTagScores(stack.getItem());

        if (itemId != null) {
            Map<String, Float> external = SourceRegistry.getExternalClassification(itemId);
            if (external != null && !external.isEmpty()) {
                tagMatches = external;
            }
        }

        // Intentional stricter gate for the live gameplay path (gate reconciliation decision —
        // see migration notes): RuntimeResolver natively gates on sourceItemFilter(), which is
        // broader than this. This check preserves the old RuntimeFoodResolver behavior so live
        // player-facing values don't change. Do not remove as "leftover duplication."
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        Map<String, Float> resolved = (food == null || food.nutrition() <= 0)
                ? Map.of()
                : RuntimeResolver.getInstance().resolve(stack, recipeManager);

        Map<String, Float> classification;
        if (tagMatches.isEmpty()) {
            classification = resolved.isEmpty() ? Map.of() : resolved;
        } else if (resolved.isEmpty()) {
            classification = tagMatches;
        } else {
            classification = TagRuntimeBlend.blend(tagMatches, resolved).result();
        }

        if (override.isEmpty()) {
            return classification;
        }

        Map<String, Float> merged = new java.util.LinkedHashMap<>(classification);
        merged.putAll(override.get().nutrients());
        return Map.copyOf(merged);
    }
}
