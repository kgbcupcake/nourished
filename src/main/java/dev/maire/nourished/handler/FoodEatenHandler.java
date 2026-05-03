package dev.maire.nourished.handler;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.attachment.NutritionAttachment;
import dev.maire.nourished.attachment.NutritionData;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.FoodNutritionRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class FoodEatenHandler {

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) return;

        FoodNutritionRegistry.NutrientValues values =
                FoodNutritionRegistry.getNutrients(stack, player.level(), false);

        NutritionData data = player.getData(NutritionAttachment.NUTRITION);
        data.addProtein(values.protein());
        data.addCarbs(values.carbs());
        data.addFats(values.fats());
        data.addVitamins(values.vitamins());
        data.addHydration(values.hydration());

        DietData diet = player.getData(DietAttachment.DIET.get());
        diet.tick();
        FoodNutritionRegistry.DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                stack, player.level(), values, food.nutrition(), food.saturation());
        float caloriesAdded = delta.calories();
        Nourished.LOGGER.debug("Nourished calories: adding {} for {}", caloriesAdded,
                stack.getItem().getDescriptionId());
        diet.addCalories(caloriesAdded);
        diet.addNutrient("fruits", delta.fruits());
        diet.addNutrient("vegetables", delta.vegetables());
        diet.addNutrient("proteins", delta.proteins());
        diet.addNutrient("grains", delta.grains());
        diet.addNutrient("sugars", delta.sugars());
        diet.addNutrient("dairy", delta.dairy());

        ModNetworking.syncDiet(player, diet);

        Nourished.LOGGER.debug("{} ate {} -> {} | {}",
                player.getName().getString(),
                stack.getItem().getDescriptionId(),
                data,
                diet);
    }
}
