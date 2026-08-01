package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.core.Nourished;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.StageContext;
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
        Nourished.LOGGER.debug("[RuntimeResolver] No classification for {}, returning unclassified", itemId);
        return new ResolutionResult(
                Map.of(), Map.of(),
                List.of(), Map.of(), Map.of(),
                false, 0f, RuntimeCascadeStage.HARD_FALLBACK,
                "unclassified");
    }
}
