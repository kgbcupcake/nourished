package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.nutrition.stages.CommunityTagStage;
import dev.maire.nourished.core.nutrition.stages.HardFallbackStage;
import dev.maire.nourished.core.nutrition.stages.KeywordSuffixStage;
import dev.maire.nourished.core.nutrition.stages.NamespacePeerStage;
import dev.maire.nourished.core.nutrition.stages.RecipeInheritanceStage;

@ApiStatus.Internal
public final class NourishedResolverStages {

    private static final CommunityTagStage COMMUNITY_TAG_STAGE = new CommunityTagStage();
    private static final KeywordSuffixStage KEYWORD_SUFFIX_STAGE = new KeywordSuffixStage();

    public static final ResolutionStageHandler[] STAGES = {
            COMMUNITY_TAG_STAGE,
            KEYWORD_SUFFIX_STAGE,
            new RecipeInheritanceStage(RuntimeFoodResolver.getInstance().recipeInheritanceResolver(), COMMUNITY_TAG_STAGE, KEYWORD_SUFFIX_STAGE),
            new NamespacePeerStage(),
            new HardFallbackStage(),
    };

    private NourishedResolverStages() {}
}
