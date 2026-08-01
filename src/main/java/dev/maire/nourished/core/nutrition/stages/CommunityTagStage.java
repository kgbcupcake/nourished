package dev.maire.nourished.core.nutrition.stages;

import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.StageContext;
import dev.marie.framework.scanner.stages.CommunityTagResolutionStage;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Delegates to marie-core's {@link CommunityTagResolutionStage} instead of maintaining a second
 * copy of community-tag matching. Kept as a distinct type (rather than using
 * {@link CommunityTagResolutionStage} directly) because {@link RecipeInheritanceStage}'s
 * constructor requires this exact type.
 */
public final class CommunityTagStage implements ResolutionStageHandler {

    private final CommunityTagResolutionStage delegate = new CommunityTagResolutionStage();

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        return delegate.resolve(itemId, ctx);
    }
}
