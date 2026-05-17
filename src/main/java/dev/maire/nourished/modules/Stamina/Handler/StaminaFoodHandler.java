package dev.maire.nourished.modules.Stamina.Handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import dev.maire.nourished.modules.Stamina.Action.StaminaActionType;
import dev.maire.nourished.modules.Stamina.Action.StaminaDrainPipeline;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

@ApiStatus.Internal
public class StaminaFoodHandler {

    @SubscribeEvent
    public void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableStamina) return;

        FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(event.getItem(), player);
        if (food == null) {
            if (StaminaConfig.enableUseItem()) {
                StaminaDrainPipeline.apply(player, StaminaActionType.USE_ITEM);
            }
            return;
        }

        RawSeverity severity = RawFoodClassifier.classify(event.getItem(), player.level());
        if (severity != RawSeverity.FINE && StaminaConfig.enableRawEatPenalty()) {
            StaminaDrainPipeline.apply(player, StaminaActionType.EAT_RAW);
        } else {
            StaminaDrainPipeline.apply(player, StaminaActionType.EAT);
        }
    }

    @SubscribeEvent
    public void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableStamina) return;
        StaminaDrainPipeline.apply(player, StaminaActionType.FISH);
    }
}
