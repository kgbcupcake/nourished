package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class NourishedItems {

    private NourishedItems() {}

    public static boolean isNutritiousFood(ItemStack stack) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        return food != null && food.nutrition() > 0;
    }
}
