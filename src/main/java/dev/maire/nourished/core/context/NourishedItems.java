package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class NourishedItems {

    private NourishedItems() {}

    public static boolean isNutritiousFood(ItemStack stack) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if ((food == null || food.nutrition() <= 0) && FoodNutritionRegistry.getNutrientTagScores(stack.getItem()).isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.create(ResourceLocation.parse("c:seeds")))) {
            return false;
        }
        return true;
    }
}
