package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.ResolutionStage;
import dev.maire.nourished.core.nutrition.StageContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Stage 5: unconditional fallback that assigns full weight to the first registered nutrient key.
 * Always produces a result (never returns {@code null}).
 */
public final class HardFallbackStage implements ResolutionStageHandler {

    @Override
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        List<String> keys = NutrientRegistry.getKeys();
        String fallbackKey = keys.get(0);
        Nourished.LOGGER.debug("[RuntimeFoodResolver] Fallback for {}, no stage produced confidence", itemId);
        return new ResolutionResult(Map.of(fallbackKey, 1.0f), 0f, ResolutionStage.HARD_FALLBACK,
                "hard fallback to " + fallbackKey);
    }
}
