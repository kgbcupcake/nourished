package dev.maire.nourished.core.nutrition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import dev.marie.framework.api.ApiStatus;

/**
 * Blend helper that merges tag-derived and runtime-resolver-derived nutrient maps,
 * returning both the result and detailed trace information about discards and precedence.
 */
@ApiStatus.Internal
public final class TagRuntimeBlend {

    /**
     * Precedence outcome for a single nutrient key in the blend.
     */
    public enum Precedence {
        TAG,
        RUNTIME_SUPPLEMENT
    }

    /**
     * Immutable blend outcome with trace detail.
     */
    public record BlendOutcome(
            Map<String, Float> result,
            Map<String, Float> discardedResolver,
            Map<String, Precedence> perKeyPrecedence
    ) {
        public BlendOutcome {
            result = Map.copyOf(result);
            discardedResolver = Map.copyOf(discardedResolver);
            perKeyPrecedence = Map.copyOf(perKeyPrecedence);
        }
    }

    private TagRuntimeBlend() {}

    /**
     * Blends tag-derived and resolver-derived nutrient maps.
     *
     * <p>Tags are authoritative: they seed the result at full weight. The resolver may only
     * add nutrients <em>not</em> already keyed by tags, at {@code 0.5x} weight before normalization.
     * If both inputs have the same nutrient key, the resolver contribution is <strong>discarded</strong>.</p>
     *
     * <p>Numeric behavior matches the original {@code FoodNutritionRegistry.blendTagAndResolverResults}
     * including the {@code total <= 0f} fallback to {@code tagMatches}.</p>
     *
     * @param tagMatches tag-derived nutrient map (compat-filtered)
     * @param resolved   runtime resolver nutrient map
     * @return blend outcome with result, discarded keys, and per-key precedence
     */
    static BlendOutcome blend(Map<String, Float> tagMatches, Map<String, Float> resolved) {
        Map<String, Float> merged = new LinkedHashMap<>(tagMatches);
        Map<String, Float> discarded = new TreeMap<>();
        Map<String, Precedence> precedence = new TreeMap<>();

        for (String k : tagMatches.keySet()) {
            precedence.put(k, Precedence.TAG);
        }

        resolved.forEach((k, v) -> {
            if (tagMatches.containsKey(k)) {
                discarded.put(k, v);
            } else {
                merged.merge(k, v * 0.5f, Float::sum);
                precedence.put(k, Precedence.RUNTIME_SUPPLEMENT);
            }
        });

        float total = 0f;
        for (float v : merged.values()) total += v;

        if (total <= 0f) {
            return new BlendOutcome(tagMatches, discarded, precedence);
        }

        final float norm = total;
        Map<String, Float> result = new LinkedHashMap<>(merged.size());
        merged.forEach((k, v) -> result.put(k, v / norm));

        return new BlendOutcome(result, discarded, precedence);
    }
}
