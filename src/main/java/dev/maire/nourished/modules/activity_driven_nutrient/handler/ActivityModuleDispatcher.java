package dev.maire.nourished.modules.activity_driven_nutrient.handler;

import dev.marie.framework.api.marie.MarieEvents;
import dev.marie.framework.api.value.ValueSourceTrigger;
import dev.marie.framework.config.FeatureFlagCache;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityEffectModule;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityModuleRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Dispatches {@link MarieEvents.SourceTriggerEvent} — the existing cancellable event
 * {@code SourceApplicationPipeline.process} already posts for every trigger — to whichever
 * registered {@link ActivityEffectModule}s react to that trigger's type.
 */
public final class ActivityModuleDispatcher {

    @SubscribeEvent
    public void onSourceTrigger(MarieEvents.SourceTriggerEvent event) {
        if (!ActivityDrivenNutrientConfig.isSynced() || !ActivityDrivenNutrientConfig.get().enabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ValueSourceTrigger trigger = event.getTrigger();
        for (ActivityEffectModule module : ActivityModuleRegistry.get(trigger.type())) {
            if (!module.enabled()) {
                continue;
            }
            if (FeatureFlagCache.enableDebugLogging()) {
                Nourished.LOGGER.debug("[ActivityModuleDispatcher] {} -> module={} player={}",
                        trigger.type(), module.id(), serverPlayer.getName().getString());
            }
            if (!module.onTrigger(serverPlayer, trigger)) {
                continue;
            }
            ActivityEffectLog.Entry entry = ActivityEffectLog.record(
                    module.id(), serverPlayer.getName().getString(), describeEffect(module.id()));
            ModNetworking.sendActivityLogEntry(serverPlayer, entry);
        }
    }

    /** Human-readable "what just happened" text for the debug HUD log — keyed off each module's own config value. */
    private static String describeEffect(String moduleId) {
        ActivityDrivenNutrientConfig config = ActivityDrivenNutrientConfig.get();
        return switch (moduleId) {
            case "mining" -> ActivityEffectLog.formatDelta("Mining", -config.miningCostPerBlock());
            case "combat" -> ActivityEffectLog.formatDelta("Combat", -config.combatCostPerKill());
            case "sprint" -> ActivityEffectLog.formatDelta("Sprint", -config.sprintDecayBoost());
            case "swim" -> ActivityEffectLog.formatDelta("Swim", -config.swimDecayBoost());
            default -> moduleId;
        };
    }
}
