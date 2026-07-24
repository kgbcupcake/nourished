package dev.maire.nourished.core.nutrition;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.reporting.ExportResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class NutrientExportResolver implements ExportResolver<Item> {

    @Override
    public String resolverId() {
        return "nourished_nutrients";
    }

    @Override
    public Map<String, Object> resolve(Item item) {
        FoodProperties food = item.components().get(DataComponents.FOOD);
        if (food == null) {
            return Map.of();
        }

        ItemStack stack = new ItemStack(item);
        Map<String, Float> bars = NutrientClassificationLookup.resolveNutrientBars(stack, false, (net.minecraft.world.item.crafting.RecipeManager) null);
        if (bars.isEmpty()) {
            return Map.of();
        }

        int calories = Math.max(0, Math.round(food.nutrition() * 25f));

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> nutrients = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : bars.entrySet()) {
            nutrients.put(e.getKey(), e.getValue());
        }
        result.put("nutrients", nutrients);
        result.put("calories", calories);
        return result;
    }
}
