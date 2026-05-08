package dev.maire.nourished.nutrition.scanner;

import java.util.Map;

/**
 * Represents a single signal that contributed to food classification.
 * Immutable record for thread-safe pipeline processing.
 *
 * @param signalType The type of signal (e.g., "COMMUNITY_TAG", "NAMESPACE", "KEYWORD")
 * @param source What triggered this signal (e.g., tag name, namespace, keyword matched)
 * @param contributions Map of nutrient key to score delta contributed by this signal
 */
public record ClassificationSignal(
        String signalType,
        String source,
        Map<String, Float> contributions
) {
    public static final String TYPE_COMMUNITY_TAG = "COMMUNITY_TAG";
    public static final String TYPE_NAMESPACE = "NAMESPACE";
    public static final String TYPE_SUFFIX = "SUFFIX";
    public static final String TYPE_KEYWORD = "KEYWORD";
    public static final String TYPE_NEGATIVE_KEYWORD = "NEGATIVE_KEYWORD";
    public static final String TYPE_ARCHETYPE = "ARCHETYPE";
    public static final String TYPE_FOOD_PROPERTIES = "FOOD_PROPERTIES";
    public static final String TYPE_RECIPE_INHERITANCE = "RECIPE_INHERITANCE";
    public static final String TYPE_NAMESPACE_PEER = "NAMESPACE_PEER";

    public ClassificationSignal {
        contributions = Map.copyOf(contributions);
    }

    /**
     * Returns the total contribution magnitude (sum of absolute values).
     */
    public float totalMagnitude() {
        float sum = 0f;
        for (float v : contributions.values()) {
            sum += Math.abs(v);
        }
        return sum;
    }
}
