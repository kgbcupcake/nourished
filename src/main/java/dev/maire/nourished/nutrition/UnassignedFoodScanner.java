package dev.maire.nourished.nutrition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds edible items that have no {@code nourished:nutrients/*} tag and resolved to the registry's
 * first nutrient only (same criteria as {@code /nourished get_unassigned_foods}).
 */
public final class UnassignedFoodScanner {

    public record ScanHit(ResourceLocation itemId, String fallbackNutrient) {}

    private UnassignedFoodScanner() {}

    public static List<ScanHit> scan() {
        List<ScanHit> out = new ArrayList<>();
        List<String> keys = NutrientRegistry.getKeys();
        String fallbackKey = keys.stream().findFirst().orElse("grains");

        for (Item item : BuiltInRegistries.ITEM) {
            FoodProperties food = item.components().get(DataComponents.FOOD);
            if (food == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            Map<String, Float> bars = FoodNutritionRegistry.resolveNutrientBars(stack, false);
            if (bars.size() == 1 && bars.containsKey(fallbackKey) && !hasNutrientTag(stack)) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) {
                    out.add(new ScanHit(id, fallbackKey));
                }
            }
        }
        return out;
    }

    public static boolean hasNutrientTag(ItemStack stack) {
        var holder = stack.getItemHolder();
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagStr));
                if (holder.is(tagKey)) {
                    return true;
                }
            }
        }
        return false;
    }
}
