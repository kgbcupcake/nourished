package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class NourishedItems {

    private NourishedItems() {}

    /**
     * An item is a valid nutrition source in exactly two cases: it's genuinely food per vanilla's
     * own rules (real {@link FoodProperties}, so no mod author has to do anything), or a human
     * explicitly said so via a {@code nourished:nutrients/*} tag or the registration API. Nothing
     * else — no keyword/recipe/namespace-peer guess is allowed to grant eligibility, only to pick a
     * category once an item has already cleared one of these two tiers.
     */
    public static boolean isNutritiousFood(ItemStack stack) {
        // Item#getFoodProperties (not the raw components() lookup) is what vanilla itself checks
        // to decide whether right-clicking actually starts eating — some items carry a static FOOD
        // component for unrelated reasons (templated Item.Properties, disabled variants) but override
        // getFoodProperties() to return null, meaning they aren't really edible despite the component.
        FoodProperties food = stack.getItem().getFoodProperties(stack, null);
        boolean hasRealFoodProperties = food != null && food.nutrition() > 0;
        boolean explicitlyRegistered = !FoodNutritionRegistry.getNutrientTagScores(stack.getItem()).isEmpty();
        if (!hasRealFoodProperties && !explicitlyRegistered) {
            return false;
        }
        if (stack.is(ItemTags.create(ResourceLocation.parse("c:seeds")))) {
            return false;
        }
        return true;
    }
}
