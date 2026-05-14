package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@ApiStatus.Internal
public class FoodEatenHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (NutritionEatingHandler.isNutritionOnlyPipelinePending(player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (FoodNutritionRegistry.foodPropertiesForNutrition(stack, player) == null) {
            return;
        }

        DietData diet = player.getData(DietAttachment.DIET.get());
        long gameTimeMs = player.level().getGameTime() * 50L;
        diet.tickTime(gameTimeMs);
        diet.tick();

        FoodNutrientPipeline.process(player, stack, diet, gameTimeMs);
    }
}
