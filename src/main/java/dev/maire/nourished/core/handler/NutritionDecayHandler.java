package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedSeasonHook;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.registry.NourishedAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class NutritionDecayHandler {

    private static final java.util.concurrent.atomic.AtomicBoolean SNAPSHOT_WARN_ONCE = new java.util.concurrent.atomic.AtomicBoolean(false);

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ModuleCache.enableDecay) return;
        if (ConfigReloadHandler.isReloadInProgress()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
        if (snapshot == null) {
            if (SNAPSHOT_WARN_ONCE.compareAndSet(false, true)) {
                dev.maire.nourished.core.Nourished.LOGGER.warn("[Nourished] NutritionDecayHandler: config snapshot is null, decay skipped. Will not warn again until server restart.");
            }
            return;
        }
        int interval = Math.max(1, snapshot.decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;

        DietData data = player.getData(DietAttachment.DIET.get());
        boolean changed = false;
        for (String key : NutrientRegistry.getKeys()) {
            float rate = (float) snapshot.decayRateFor(key);
            rate = applySeasonalDecayModifier(key, rate);
            rate *= NourishedAttributes.nutrientDecayMultiplier(player);
            float current = data.nutrients.getOrDefault(key, 0f);
            if (current > 0f) {
                float newValue = Math.max(0f, current - rate);
                data.nutrients.put(key, newValue);
                changed = true;

                if (current != newValue) {
                    NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientChangedEvent(
                            player, key, current, newValue));

                    if (NutrientRegistry.isBeneficial(key)) {
                        float criticalThreshold = (float) snapshot.criticalThreshold();
                        if (newValue <= criticalThreshold && current > criticalThreshold) {
                            NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientCriticalEvent(player, key));
                        }
                    }
                }
            }
        }

        if (changed) {
            player.setData(DietAttachment.DIET.get(), data);
            ModNetworking.syncDietDelta(player, data);
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SNAPSHOT_WARN_ONCE.set(false);
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
