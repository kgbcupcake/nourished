package dev.maire.nourished.core.nutrition;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.runtime.SourceRegistry;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;

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
        if (itemId != null && ScannerSpecRegistry.get().excludedItems().contains(itemId.toString())) {
            return Map.of();
        }

        Map<String, Float> tagMatches = FoodNutritionRegistry.getNutrientTagScores(stack.getItem());

        if (itemId != null) {
            Map<String, Float> external = SourceRegistry.getExternalClassification(itemId);
            if (external != null && !external.isEmpty()) {
                tagMatches = external;
            }
        }

        Map<String, Float> resolved = RuntimeFoodResolver.getInstance().resolve(stack, recipeManager);

        if (tagMatches.isEmpty()) {
            return resolved.isEmpty() ? Map.of() : resolved;
        }
        if (resolved.isEmpty()) {
            return tagMatches;
        }
        return TagRuntimeBlend.blend(tagMatches, resolved).result();
    }
}
