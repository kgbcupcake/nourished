package dev.maire.nourished.core.tagaudit.rules;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;
import dev.marie.MariesLib.tagaudit.model.TagAuditSeverity;
import dev.marie.MariesLib.tagaudit.model.TagFixSuggestion;
import dev.marie.MariesLib.tagaudit.model.TagIssue;
import dev.marie.MariesLib.tagaudit.rule.TagRule;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flags items whose mod namespace has a strong, config-authored bias toward
 * one nutrient category (per scanner_spec.json's namespaceWeights), but are
 * bundled-tagged into a different category entirely. Independent of live
 * inference confidence — catches cases like a namespace that wasn't yet
 * registered with scanner weights at the time a bad tag was added, where
 * TagInferenceMismatchRule's live-inference comparison might not confidently
 * disagree even though the namespace bias clearly points elsewhere.
 */
@ApiStatus.Internal
public final class NamespaceBiasRule implements TagRule {

    /**
     * Minimum share of a namespace's total weight one category must hold
     * before this rule considers that namespace "biased" enough to compare
     * against. A namespace with no clear lean (weight spread evenly across
     * categories) produces no findings — only a strong, unambiguous bias
     * is worth flagging a disagreement against.
     */
    private static final float MIN_NAMESPACE_BIAS_SHARE = 0.7f;

    @Override
    public String ruleId() {
        return "nourished_namespace_bias";
    }

    @Override
    public List<TagIssue> findIssues(TagAuditContext context) {
        List<TagIssue> issues = new ArrayList<>();
        Map<String, Map<String, Float>> namespaceWeights = ScannerSpecRegistry.get().namespaceWeights();
        if (namespaceWeights.isEmpty()) {
            return issues;
        }

        for (String category : context.knownCategories()) {
            for (ResourceLocation itemId : context.itemsInCategory(category)) {
                String namespace = itemId.getNamespace();
                Map<String, Float> weights = namespaceWeights.get(namespace);
                if (weights == null || weights.isEmpty()) {
                    continue;
                }

                String biasedCategory = topScoringKey(weights);
                if (biasedCategory == null || biasedCategory.equals(category)) {
                    continue;
                }

                float share = shareOf(weights, biasedCategory);
                if (share < MIN_NAMESPACE_BIAS_SHARE) {
                    continue;
                }

                String issueId = ruleId() + ":" + itemId + ":" + category;
                TagAuditSeverity severity = share >= 0.9f
                        ? TagAuditSeverity.MEDIUM
                        : TagAuditSeverity.LOW;
                // Note: namespace bias alone is a weaker signal than live inference
                // disagreement (TagInferenceMismatchRule), so this rule caps at
                // MEDIUM severity even at high bias share — it's a "worth reviewing"
                // signal, not a confirmed mistake (e.g. nuts-as-protein from a
                // fruit-biased namespace can be a legitimate, defensible exception).

                issues.add(new TagIssue(
                        issueId,
                        itemId,
                        category,
                        ruleId(),
                        share,
                        severity,
                        String.format(
                                "Namespace '%s' is %.0f%% biased toward '%s' per scanner_spec.json, but this item is tagged '%s'",
                                namespace, share * 100f, biasedCategory, category)
                ));
            }
        }
        return issues;
    }

    @Override
    public List<TagFixSuggestion> suggestFixes(TagAuditContext context, List<TagIssue> issues) {
        List<TagFixSuggestion> suggestions = new ArrayList<>();
        Map<String, Map<String, Float>> namespaceWeights = ScannerSpecRegistry.get().namespaceWeights();

        for (TagIssue issue : issues) {
            if (!ruleId().equals(issue.ruleId())) {
                continue;
            }
            String namespace = issue.itemId().getNamespace();
            Map<String, Float> weights = namespaceWeights.get(namespace);
            if (weights == null || weights.isEmpty()) {
                continue;
            }
            String biasedCategory = topScoringKey(weights);
            if (biasedCategory == null) {
                continue;
            }
            float share = shareOf(weights, biasedCategory);
            suggestions.add(new TagFixSuggestion(
                    issue.issueId(),
                    biasedCategory,
                    ruleId(),
                    // Deliberately lower confidence than the issue's own severity
                    // implies — namespace bias is a weaker, second-order signal
                    // for SUGGESTING a fix than for merely flagging a review,
                    // since it's more likely to have legitimate exceptions
                    // (e.g. nuts from a fruit-biased namespace).
                    share * 0.75f,
                    "Consider moving to '" + biasedCategory + "' to match namespace bias — review before applying, "
                            + "this signal alone can have legitimate exceptions"
            ));
        }
        return suggestions;
    }

    private static String topScoringKey(Map<String, Float> weights) {
        String best = null;
        float bestVal = Float.NEGATIVE_INFINITY;
        for (Map.Entry<String, Float> e : weights.entrySet()) {
            if (e.getValue() > bestVal) {
                bestVal = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static float shareOf(Map<String, Float> weights, String key) {
        float total = 0f;
        for (float v : weights.values()) {
            total += Math.max(0f, v);
        }
        if (total <= 0f) {
            return 0f;
        }
        Float value = weights.get(key);
        return value == null ? 0f : Math.max(0f, value) / total;
    }
}
