package dev.maire.nourished.modules.activity_driven_nutrient.modules;

import dev.marie.framework.api.value.ValueSourceTrigger;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityEffectModule;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityNutrientEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies {@code sprintDecayBoost} on top of passive decay each time marie-core's
 * {@code ValueEffectsListener} fires a {@code TICK} trigger with source id {@code "sprint"} (i.e.
 * while the player is sprinting) — an additional periodic subtraction alongside normal passive
 * decay, not a modification of it.
 */
public final class SprintDecayModule implements ActivityEffectModule {

    private static final String TICK_SOURCE_ID = "sprint";

    @Override
    public String id() {
        return "sprint";
    }

    @Override
    public ValueSourceTrigger.TriggerType triggerType() {
        return ValueSourceTrigger.TriggerType.TICK;
    }

    @Override
    public boolean enabled() {
        return ActivityDrivenNutrientConfig.get().sprintEnabled();
    }

    @Override
    public boolean onTrigger(ServerPlayer player, ValueSourceTrigger trigger) {
        if (!TICK_SOURCE_ID.equals(trigger.sourceId())) {
            return false;
        }
        ActivityNutrientEffects.applyUniformDelta(player, -ActivityDrivenNutrientConfig.get().sprintDecayBoost());
        return true;
    }
}
