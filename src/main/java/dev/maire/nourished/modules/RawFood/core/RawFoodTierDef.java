package dev.maire.nourished.modules.RawFood.core;

import dev.marie.MariesLib.api.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public record RawFoodTierDef(
        List<String> effectPool,
        int durationTicks,
        int amplifier,
        float nutrientPenalty,
        float missedOpportunityMultiplier
) {}
