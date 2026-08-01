package dev.maire.nourished.modules.activity_driven_nutrient.core;

import dev.marie.framework.api.value.ValueSourceTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * One activity-driven nutrient effect (sprint, swim, mining, combat, starvation, …). A concrete
 * module reacts to a single {@link ValueSourceTrigger.TriggerType} and applies its own effect when
 * triggered — dispatch (matching triggers to registered modules) is {@link ActivityModuleRegistry}'s
 * job, and config lookup is each module's own job via {@link #enabled()}: this interface stays
 * config-agnostic so the registry never needs to know about {@code ActivityDrivenNutrientConfig}.
 */
public interface ActivityEffectModule {

    /** Short, human-readable, stable identifier for this module (e.g. {@code "sprint"}, {@code "mining"}). */
    String id();

    /** Which {@link ValueSourceTrigger.TriggerType} this module reacts to. */
    ValueSourceTrigger.TriggerType triggerType();

    /** Whether this module is currently active — implementations read their own config getter. */
    boolean enabled();

    /** Applies this module's effect for {@code trigger}. */
    void onTrigger(ServerPlayer player, ValueSourceTrigger trigger);
}
