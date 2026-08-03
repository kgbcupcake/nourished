package dev.maire.nourished.modules.activity_driven_nutrient.modules;

import dev.marie.framework.api.value.ValueSourceTrigger;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityEffectModule;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityNutrientEffects;
import net.minecraft.server.level.ServerPlayer;

/** Applies {@code combatCostPerKill} once per {@link ValueSourceTrigger.TriggerType#ENTITY_KILLED}. */
public final class CombatModule implements ActivityEffectModule {

    @Override
    public String id() {
        return "combat";
    }

    @Override
    public ValueSourceTrigger.TriggerType triggerType() {
        return ValueSourceTrigger.TriggerType.ENTITY_KILLED;
    }

    @Override
    public boolean enabled() {
        return ActivityDrivenNutrientConfig.get().combatEnabled();
    }

    @Override
    public boolean onTrigger(ServerPlayer player, ValueSourceTrigger trigger) {
        ActivityNutrientEffects.applyUniformDelta(player, -ActivityDrivenNutrientConfig.get().combatCostPerKill());
        return true;
    }
}
