package dev.maire.nourished.core.nutrition;

/**
 * Identifies which stage of the runtime inference cascade produced a classification.
 * This enum tracks the internal resolution pipeline within {@link RuntimeFoodResolver}.
 */
public enum RuntimeCascadeStage {
    COMMUNITY_TAG,
    KEYWORD_SUFFIX,
    COMPOSITE,
    COMPOSITE_RECIPE,
    KEYWORD_SUFFIX_RECIPE,
    RECIPE_INHERITANCE,
    NAMESPACE_PEER,
    HARD_FALLBACK;

    /**
     * Human-readable label for debug output (e.g. {@code COMPOSITE+RECIPE}).
     */
    public String displayName() {
        return switch (this) {
            case COMPOSITE_RECIPE -> "COMPOSITE+RECIPE";
            case KEYWORD_SUFFIX_RECIPE -> "KEYWORD_SUFFIX+RECIPE";
            default -> name();
        };
    }
}
