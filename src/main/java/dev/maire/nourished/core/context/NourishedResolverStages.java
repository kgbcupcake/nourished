package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.scan.ResolutionStageHandler;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.nutrition.stages.CommunityTagStage;
import dev.maire.nourished.core.nutrition.stages.HardFallbackStage;
import dev.maire.nourished.core.nutrition.stages.KeywordSuffixStage;
import dev.maire.nourished.core.nutrition.stages.NamespacePeerStage;
import dev.maire.nourished.core.nutrition.stages.RecipeInheritanceStage;

@ApiStatus.Internal
public final class NourishedResolverStages {

    public static final ResolutionStageHandler[] STAGES = {
            new CommunityTagStage(),
            new KeywordSuffixStage(),
            new RecipeInheritanceStage(RuntimeFoodResolver.getInstance().recipeInheritanceResolver()),
            new NamespacePeerStage(),
            new HardFallbackStage(),
    };

    private NourishedResolverStages() {}
}
