package dev.maire.nourished.modules.activity_driven_nutrient.modules;

import dev.marie.framework.api.value.ValueSourceTrigger;
import dev.marie.framework.tracking.TrackerMilestoneTracker;
import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityEffectModule;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityTrackerIds;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityNutrientEffects;
import net.minecraft.server.level.ServerPlayer;

/** Applies {@code miningCostPerBlock} once per {@link ValueSourceTrigger.TriggerType#BLOCK_BROKEN}. */
public final class MiningModule implements ActivityEffectModule {

    @Override
    public String id() {
        return "mining";
    }

    @Override
    public ValueSourceTrigger.TriggerType triggerType() {
        return ValueSourceTrigger.TriggerType.BLOCK_BROKEN;
    }

    @Override
    public boolean enabled() {
        return ActivityDrivenNutrientConfig.get().miningEnabled();
    }

    @Override
    public boolean onTrigger(ServerPlayer player, ValueSourceTrigger trigger) {
        ActivityNutrientEffects.applyUniformDelta(player, -ActivityDrivenNutrientConfig.get().miningCostPerBlock());
        MarieTracking.incrementTracker(player, ActivityTrackerIds.MINING_BLOCKS_ID, 1f);
        TrackerMilestoneTracker.onTrackerIncremented(player, ActivityTrackerIds.MINING_BLOCKS_ID, 1f);
        return true;
    }
}
