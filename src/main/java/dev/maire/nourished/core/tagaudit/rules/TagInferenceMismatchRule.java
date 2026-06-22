package dev.maire.nourished.core.tagaudit.rules;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;
import dev.marie.MariesLib.tagaudit.model.TagAuditSeverity;
import dev.marie.MariesLib.tagaudit.model.TagFixSuggestion;
import dev.marie.MariesLib.tagaudit.model.TagIssue;
import dev.marie.MariesLib.tagaudit.rule.TagRule;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Flags items whose bundled nutrient tag disagrees with what live, tag-independent
 * runtime inference would currently say. Catches cases like
 * {@code farmerscroptopia:fruitsdelight/pear_with_rock_sugar} — bundled-tagged as
 * proteins, but live inference confidently resolves it as fruits — which are
 * otherwise invisible because bundled tags are authoritative over runtime inference
 * in normal gameplay, so a wrong tag never self-corrects.
 */
@ApiStatus.Internal
public final class TagInferenceMismatchRule implements TagRule {

    /**
     * Minimum live-inference confidence (top score / total of all scores) required
     * before this rule trusts the inference result enough to flag a disagreement.
     * Low-confidence/ambiguous inference results are not reliable enough to
     * contradict an existing bundled tag.
     */
    private static final float MIN_INFERENCE_CONFIDENCE = 0.6f;

    @Override
    public String ruleId() {
        return "nourished_tag_inference_mismatch";
    }

    @Override
    public List<TagIssue> findIssues(TagAuditContext context) {
        List<TagIssue> issues = new ArrayList<>();
        Function<ResourceLocation, Map<String, Float>> liveInference = context.liveInferenceLookup();
        if (liveInference == null) {
            // No live-inference signal available from this context — this rule
            // has nothing to compare against, so it finds nothing rather than
            // guessing. Other rules can still run independently.
            return issues;
        }

        for (String category : context.knownCategories()) {
            for (ResourceLocation itemId : context.itemsInCategory(category)) {
                Map<String, Float> inferred = liveInference.apply(itemId);
                if (inferred == null || inferred.isEmpty()) {
                    continue;
                }

                String topCategory = topScoringKey(inferred);
                if (topCategory == null || topCategory.equals(category)) {
                    continue;
                }

                float confidence = confidenceOf(inferred, topCategory);
                if (confidence < MIN_INFERENCE_CONFIDENCE) {
                    continue;
                }

                String issueId = ruleId() + ":" + itemId + ":" + category;
                TagAuditSeverity severity = confidence >= 0.85f
                        ? TagAuditSeverity.HIGH
                        : TagAuditSeverity.MEDIUM;

                issues.add(new TagIssue(
                        issueId,
                        itemId,
                        category,
                        ruleId(),
                        confidence,
                        severity,
                        String.format(
                                "Tagged as '%s' but live inference resolves to '%s' at %.0f%% confidence",
                                category, topCategory, confidence * 100f)
                ));
            }
        }
        return issues;
    }

    @Override
    public List<TagFixSuggestion> suggestFixes(TagAuditContext context, List<TagIssue> issues) {
        List<TagFixSuggestion> suggestions = new ArrayList<>();
        Function<ResourceLocation, Map<String, Float>> liveInference = context.liveInferenceLookup();
        if (liveInference == null) {
            return suggestions;
        }

        for (TagIssue issue : issues) {
            if (!ruleId().equals(issue.ruleId())) {
                continue;
            }
            Map<String, Float> inferred = liveInference.apply(issue.itemId());
            if (inferred == null || inferred.isEmpty()) {
                continue;
            }
            String topCategory = topScoringKey(inferred);
            if (topCategory == null) {
                continue;
            }
            float confidence = confidenceOf(inferred, topCategory);
            suggestions.add(new TagFixSuggestion(
                    issue.issueId(),
                    topCategory,
                    ruleId(),
                    confidence,
                    "Move from '" + issue.currentTag() + "' to '" + topCategory + "' per live inference"
            ));
        }
        return suggestions;
    }

    /** Returns the key with the highest value in the map, or null if empty. */
    private static String topScoringKey(Map<String, Float> scores) {
        String best = null;
        float bestVal = Float.NEGATIVE_INFINITY;
        for (Map.Entry<String, Float> e : scores.entrySet()) {
            if (e.getValue() > bestVal) {
                bestVal = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** Confidence as the named key's share of the total of all values (0 if total <= 0). */
    private static float confidenceOf(Map<String, Float> scores, String key) {
        float total = 0f;
        for (float v : scores.values()) {
            total += Math.max(0f, v);
        }
        if (total <= 0f) {
            return 0f;
        }
        Float value = scores.get(key);
        return value == null ? 0f : Math.max(0f, value) / total;
    }
}
