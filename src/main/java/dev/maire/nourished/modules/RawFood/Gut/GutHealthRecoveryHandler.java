package dev.maire.nourished.modules.RawFood.Gut;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.config.NourishedModuleCache;
import dev.maire.nourished.core.NourishedKubeIntegration;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.rawInfo.CookednessResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Handles immediate gut recovery from eating cooked food.
 *
 * <p>Subscribes to {@link LivingEntityUseItemEvent.Finish}. When the player
 * eats food with cookedness >= 0.5, applies recovery proportional to the
 * cookedness level.</p>
 */
@ApiStatus.Internal
public class GutHealthRecoveryHandler {

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!NourishedModuleCache.enableGutHealth) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();
        if (FoodNutritionRegistry.foodPropertiesForNutrition(stack, player) == null) {
            return;
        }

        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);
        float cookedness = CookednessResolver.resolve(itemId);

        if (cookedness < 0.5f) {
            return;
        }

        float recoveryAmount = RawFoodConfig.cookedFoodRecoveryRate() * cookedness;

        GutHealthData gut = player.getData(GutHealthAttachment.GUT.get());
        float oldGutHealth = gut.getGutHealth();
        gut.applyRecovery(recoveryAmount);
        gut.setLastUpdateMs(player.level().getGameTime() * 50L);

        player.setData(GutHealthAttachment.GUT.get(), gut);
        ModNetworking.syncGutHealth(player, gut);
        NourishedKubeIntegration.fireGutHealthChanged(
                player.getUUID().toString(), oldGutHealth, gut.getGutHealth(), "recovery");
    }
}
