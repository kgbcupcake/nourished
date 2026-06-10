package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieSeasonHook;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.MariesLib.registry.MarieAttributes;
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
        TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
        if (snapshot != null) {
            data.setMemoryConfig(new TrackingMemoryConfig(
                    snapshot.memoryWindowMinutes(), snapshot.noveltyBonus(), snapshot.noveltyDecayCap(),
                    snapshot.diminishingFloor(), snapshot.startingNutrientValue()));
        } else {
            if (SNAPSHOT_WARN_ONCE.compareAndSet(false, true)) {
                dev.maire.nourished.core.Nourished.LOGGER.warn("[Nourished] NutritionDecayHandler: config snapshot is null, decay skipped. Will not warn again until server restart.");
            }
            NourishedConfig cfg = NourishedConfig.get();
            data.setMemoryConfig(new TrackingMemoryConfig(
                    cfg.memoryWindowMinutes(), cfg.noveltyBonus(), cfg.noveltyDecayCap(),
                    cfg.diminishingFloor(), cfg.startingNutrientValue()));
            return;
        }
        int interval = Math.max(1, snapshot.decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;
        boolean changed = false;
        for (String key : NutrientRegistry.getKeys()) {
            float rate = (float) snapshot.decayRateFor(key);
            rate = applySeasonalDecayModifier(key, rate);
            rate *= MarieAttributes.valueDecayMultiplier(player);
            float current = data.values.getOrDefault(key, 0f);
            if (current > 0f) {
                float newValue = Math.max(0f, current - rate);
                data.values.put(key, newValue);
                changed = true;

                if (current != newValue) {
                    NeoForge.EVENT_BUS.post(new MarieEvents.ValueChangedEvent(
                            player, key, current, newValue));

                    if (NutrientRegistry.isBeneficial(key)) {
                        float criticalThreshold = (float) snapshot.criticalThreshold();
                        if (newValue <= criticalThreshold && current > criticalThreshold) {
                            NeoForge.EVENT_BUS.post(new MarieEvents.ValueCriticalEvent(player, key));
                        }
                    }
                }
            }
        }

        if (changed) {
            player.setData(TrackingAttachment.TRACKING.get(), data);
            ModNetworking.syncDietDelta(player, data);
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SNAPSHOT_WARN_ONCE.set(false);
        FoodNutrientPipeline.resetSnapshotWarnings();
    }

    private float applySeasonalDecayModifier(String valueKey, float baseRate) {
        var hooks = SeasonHookRegistry.getAll();
        if (!ModuleCache.enableSeasonHooks || hooks.isEmpty()) {
            return baseRate;
        }
        float rate = baseRate;
        for (MarieSeasonHook hook : hooks) {
            float seasonal = Math.max(0f, hook.getSeasonalDecayModifier(valueKey, MarieSeasonHook.Season.SPRING));
            rate *= seasonal;
        }
        return rate;
    }
}
