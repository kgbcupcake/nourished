package dev.maire.nourished.modules.activity_driven_nutrient.handler;

import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared mutation path for the activity-driven nutrient modules — mirrors the same
 * get/mutate/setData/syncDietDelta/apply sequence {@code RawFoodPenaltyHandler} already uses for
 * one-off nutrient penalties, rather than inventing a new one.
 */
public final class ActivityNutrientEffects {

    private ActivityNutrientEffects() {}

    /**
     * Adds {@code amount} (negative for a cost) to every registered nutrient bar, then syncs the
     * delta to the client and re-evaluates nutrition effects. No-ops if tracking data isn't
     * registered yet or {@code amount} is zero.
     */
    public static void applyUniformDelta(ServerPlayer player, float amount) {
        if (amount == 0f || !TrackingAttachment.isRegistered()) {
            return;
        }
        TrackingData diet = TrackingAttachment.getData(player);
        for (String key : NutrientRegistry.getKeys()) {
            diet.addValue(key, amount);
        }
        TrackingAttachment.setData(player, diet);
        ModNetworking.syncDietDelta(player, diet);
        NutritionEffectApplier.apply(player, diet);
    }
}
