package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.api.ApiStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges keyword/composite primary resolution with supplementary recipe inheritance.
 * Recipe nutrients are add-only — existing primary keys are never overwritten.
 */
@ApiStatus.Internal
public final class RuntimeResolutionMerge {

    private RuntimeResolutionMerge() {}

    /**
     * Combines a keyword/composite primary result with recipe inheritance output.
     *
     * @return merged result, primary-only, recipe-only, or {@code null} when both inputs are null
     */
    @Nullable
    public static ResolutionResult mergePrimaryWithRecipeSupplement(
            @Nullable ResolutionResult primary,
            @Nullable ResolutionResult recipe
    ) {
        if (primary == null) {
            return recipe;
        }
        if (recipe == null) {
            return primary;
        }

        Map<String, Float> merged = new LinkedHashMap<>(primary.nutrients());
        List<String> addedKeys = new ArrayList<>();
        for (Map.Entry<String, Float> e : recipe.nutrients().entrySet()) {
            if (!merged.containsKey(e.getKey())) {
                merged.put(e.getKey(), e.getValue());
                addedKeys.add(e.getKey());
            }
        }
        if (addedKeys.isEmpty()) {
            return primary;
        }

        Map<String, String> rejected = new LinkedHashMap<>(primary.rejectedSignals());
        for (String key : addedKeys) {
            rejected.remove(key);
        }

        RuntimeCascadeStage stage = compoundStage(primary.stage());
        String debugReason = primary.debugReason() + "; recipe supplement added [" + String.join(",", addedKeys) + "]";

        return new ResolutionResult(
                merged,
                primary.rawScores(),
                primary.tokens(),
                primary.tokenWeights(),
                rejected,
                primary.cacheHit(),
                primary.confidence(),
                stage,
                debugReason
        );
    }

    private static RuntimeCascadeStage compoundStage(RuntimeCascadeStage primaryStage) {
        return switch (primaryStage) {
            case COMPOSITE -> RuntimeCascadeStage.COMPOSITE_RECIPE;
            case KEYWORD_SUFFIX -> RuntimeCascadeStage.KEYWORD_SUFFIX_RECIPE;
            default -> primaryStage;
        };
    }
}
