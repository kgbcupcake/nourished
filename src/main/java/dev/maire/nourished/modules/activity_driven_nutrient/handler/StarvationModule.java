package dev.maire.nourished.modules.activity_driven_nutrient.handler;

import dev.marie.framework.api.marie.MarieEvents;
import dev.marie.framework.config.FeatureFlagCache;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityEffectModule;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityModuleRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Applies {@code starvationPenalty} once, at the moment a nutrient crosses into critical.
 *
 * <p>Does not implement {@link ActivityEffectModule} — {@link MarieEvents.ValueCriticalEvent}
 * doesn't carry a {@code ValueSourceTrigger}, so it can't go through {@link
 * ActivityModuleDispatcher}/{@link ActivityModuleRegistry} — and is edge-triggered by construction
 * (see {@code ThresholdCrossingEvaluator}: fired only on the current-vs-previous crossing, not every
 * tick while critical), so this is naturally a one-time reaction rather than a continuous effect.
 * Since it bypasses the shared dispatcher, it must repeat the master/isSynced/module-enabled guards
 * the dispatcher would otherwise provide.
 */
public final class StarvationModule {

    @SubscribeEvent
    public void onValueCritical(MarieEvents.ValueCriticalEvent event) {
        if (!ActivityDrivenNutrientConfig.isSynced()) {
            return;
        }
        ActivityDrivenNutrientConfig config = ActivityDrivenNutrientConfig.get();
        if (!config.enabled() || !config.starvationEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (FeatureFlagCache.enableDebugLogging()) {
            Nourished.LOGGER.debug("[StarvationModule] critical crossing valueKey={} player={}",
                    event.getValueKey(), serverPlayer.getName().getString());
        }
        ActivityNutrientEffects.applyUniformDelta(serverPlayer, -config.starvationPenalty());
        ActivityEffectLog.record("starvation", serverPlayer.getName().getString(),
                ActivityEffectLog.formatDelta("Starvation", -config.starvationPenalty()));
    }
}
