package dev.maire.nourished.core.nutrition;

/**
 * Identifies which stage of the runtime inference cascade produced a classification.
 * This enum tracks the internal resolution pipeline within {@link RuntimeFoodResolver}.
 */
public enum RuntimeCascadeStage {
    COMMUNITY_TAG,
    KEYWORD_SUFFIX,
    COMPOSITE,
    RECIPE_INHERITANCE,
    NAMESPACE_PEER,
    HARD_FALLBACK
}
