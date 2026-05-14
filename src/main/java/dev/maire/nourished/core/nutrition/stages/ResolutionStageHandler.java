package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.StageContext;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * A single step in the runtime food resolution cascade.
 * Implementations must be stateless with respect to the resolution call (all mutable
 * context lives in {@link StageContext}) and must return {@code null} to defer to the next stage.
 */
public interface ResolutionStageHandler {

    @Nullable
    ResolutionResult resolve(ResourceLocation itemId, StageContext ctx);
}
