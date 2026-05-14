package dev.maire.nourished.core.nutrition;

/**
 * Identifies which stage of the runtime inference cascade produced a classification.
 */
public enum ResolutionStage {
    COMMUNITY_TAG,
    KEYWORD_SUFFIX,
    COMPOSITE,
    RECIPE_INHERITANCE,
    NAMESPACE_PEER,
    HARD_FALLBACK
}
