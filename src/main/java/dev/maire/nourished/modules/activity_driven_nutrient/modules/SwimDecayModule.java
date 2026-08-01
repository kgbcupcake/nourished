package dev.maire.nourished.modules.activity_driven_nutrient.modules;

import dev.marie.framework.api.value.ValueSourceTrigger;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityEffectModule;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityNutrientEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies {@code swimDecayBoost} on top of passive decay each time marie-core's
 * {@code ValueEffectsListener} fires a {@code TICK} trigger with source id {@code "swim"} (i.e.
 * while the player is swimming) — an additional periodic subtraction alongside normal passive
 * decay, not a modification of it.
 */
public final class SwimDecayModule implements ActivityEffectModule {

    private static final String TICK_SOURCE_ID = "swim";

    @Override
    public String id() {
        return "swim";
    }

    @Override
    public ValueSourceTrigger.TriggerType triggerType() {
        return ValueSourceTrigger.TriggerType.TICK;
    }

    @Override
    public boolean enabled() {
        return ActivityDrivenNutrientConfig.get().swimEnabled();
    }

    @Override
    public void onTrigger(ServerPlayer player, ValueSourceTrigger trigger) {
        if (!TICK_SOURCE_ID.equals(trigger.sourceId())) {
            return;
        }
        ActivityNutrientEffects.applyUniformDelta(player, -ActivityDrivenNutrientConfig.get().swimDecayBoost());
    }
}
