package dev.maire.nourished.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedSeasonHook;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.network.ModNetworking;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class NutritionDecayHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ModuleCache.enableDecay) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NourishedConfig config = NourishedConfig.get();
        int interval = Math.max(1, config.decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;

        DietData data = player.getData(DietAttachment.DIET.get());
        boolean changed = false;
        for (String key : NutrientRegistry.getKeys()) {
            float rate = (float) config.decayRateFor(key);
            rate = applySeasonalDecayModifier(key, rate);
            float current = data.nutrients.getOrDefault(key, 0f);
            if (current > 0f) {
                float newValue = Math.max(0f, current - rate);
                data.nutrients.put(key, newValue);
                changed = true;

                if (current != newValue) {
                    NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientChangedEvent(
                            player, key, current, newValue));

                    float criticalThreshold = (float) config.criticalThresholdFor(key);
                    if (newValue <= criticalThreshold && current > criticalThreshold) {
                        NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientCriticalEvent(player, key));
                    }
                }
            }
        }

        if (changed) {
            player.setData(DietAttachment.DIET.get(), data);
            ModNetworking.syncDietDelta(player, data);
        }
    }

    private float applySeasonalDecayModifier(String nutrientKey, float baseRate) {
        var hooks = SeasonHookRegistry.getAll();
        if (!ModuleCache.enableSeasonHooks || hooks.isEmpty()) {
            return baseRate;
        }
        float rate = baseRate;
        for (NourishedSeasonHook hook : hooks) {
            float seasonal = Math.max(0f, hook.getSeasonalDecayModifier(nutrientKey, NourishedSeasonHook.Season.SPRING));
            rate *= seasonal;
        }
        return rate;
    }
}
