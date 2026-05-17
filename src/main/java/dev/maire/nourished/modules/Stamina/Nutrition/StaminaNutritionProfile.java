package dev.maire.nourished.modules.Stamina.Nutrition;

import dev.maire.nourished.api.ApiStatus;

@ApiStatus.Internal
public record StaminaNutritionProfile(
        float proteinLevel,
        float grainLevel,
        float vegetableLevel,
        float dairyLevel,
        float gutHealth,
        float balanceScore,
        float physicalModifier,
        float mentalModifier,
        float regenModifier,
        float fatigueResistance
) {}
