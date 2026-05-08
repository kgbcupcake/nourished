package dev.maire.nourished.handler;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.effect.NutritionEffectApplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps nutrition-linked mob effects in sync with diet data independently of nutrient decay.
 * Decay being disabled must not strand effects or skip {@link NutritionEffectApplier#apply}.
 */
public class NutritionEffectsHandler {

    private static final int APPLY_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % APPLY_INTERVAL_TICKS != 0) return;

        NourishedConfig config = NourishedConfig.get();
        if (config.enableEffects()) {
            DietData data = player.getData(DietAttachment.DIET.get());
            NutritionEffectApplier.apply(player, data);
        } else {
            NutritionEffectApplier.clearAll(player);
        }
    }
}
