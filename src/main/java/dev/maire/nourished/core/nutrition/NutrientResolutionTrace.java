package dev.maire.nourished.core.nutrition;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable verbose resolution trace for debugging. Built during resolution and attached to debug context.
 *
 * <p>All map keys are sorted (TreeMap) and floats formatted via {@code Locale.ROOT} for deterministic,
 * human-readable output stable between runs.</p>
 */
public record NutrientResolutionTrace(
        String itemId,
        Map<String, Float> tagDerivedRaw,
        Map<String, Float> tagDerivedFiltered,
        List<String> strippedByCompat,
        Map<String, Float> externalMap,
        ExternalSource externalSource,
        Map<String, Float> resolverNutrients,
        @Nullable RuntimeCascadeStage cascadeStage,
        Map<String, String> resolverRejectedSignals,
        Map<String, Float> blendTagInput,
        Map<String, Float> blendResolverInput,
        Map<String, TagRuntimeBlend.Precedence> blendPrecedence,
        Map<String, Float> blendDiscardedResolver,
        Map<String, Float> finalMergedBars,
        ResolutionStage stage
) {

    public enum ExternalSource {
        NONE,
        API,
        SCANNER
    }

    public NutrientResolutionTrace {
        tagDerivedRaw = sortedCopy(tagDerivedRaw);
        tagDerivedFiltered = sortedCopy(tagDerivedFiltered);
        strippedByCompat = List.copyOf(strippedByCompat);
        externalMap = sortedCopy(externalMap);
        resolverNutrients = sortedCopy(resolverNutrients);
        resolverRejectedSignals = sortedCopyStrings(resolverRejectedSignals);
        blendTagInput = sortedCopy(blendTagInput);
        blendResolverInput = sortedCopy(blendResolverInput);
        blendPrecedence = Map.copyOf(blendPrecedence);
        blendDiscardedResolver = sortedCopy(blendDiscardedResolver);
        finalMergedBars = sortedCopy(finalMergedBars);
    }

    private static Map<String, Float> sortedCopy(Map<String, Float> m) {
        return m == null || m.isEmpty() ? Map.of() : Map.copyOf(new TreeMap<>(m));
    }

    private static Map<String, String> sortedCopyStrings(Map<String, String> m) {
        return m == null || m.isEmpty() ? Map.of() : Map.copyOf(new TreeMap<>(m));
    }

    /**
     * Formats the trace as human-readable multi-line text for debug output and file dump.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Nutrient Resolution Trace ===\n");
        sb.append("Item: ").append(itemId).append("\n");
        sb.append("Stage: ").append(stage.name()).append("\n\n");

        sb.append("--- Tag-Derived Nutrients ---\n");
        sb.append("Raw (before compat filter):\n");
        appendMapIndented(sb, tagDerivedRaw);
        sb.append("After compat filter:\n");
        appendMapIndented(sb, tagDerivedFiltered);
        if (!strippedByCompat.isEmpty()) {
            sb.append("Stripped by compat: ").append(String.join(", ", strippedByCompat)).append("\n");
        }
        sb.append("\n");

        sb.append("--- External Classification ---\n");
        sb.append("Source: ").append(externalSource.name()).append("\n");
        appendMapIndented(sb, externalMap);
        sb.append("\n");

        sb.append("--- Runtime Resolver ---\n");
        if (cascadeStage != null) {
            sb.append("Cascade stage: ").append(cascadeStage.displayName()).append("\n");
        }
        sb.append("Nutrients:\n");
        appendMapIndented(sb, resolverNutrients);
        if (!resolverRejectedSignals.isEmpty()) {
            sb.append("Rejected signals:\n");
            for (Map.Entry<String, String> e : resolverRejectedSignals.entrySet()) {
                sb.append("  ").append(e.getKey()).append(" -> ").append(e.getValue()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("--- Blend ---\n");
        sb.append("Tag input:\n");
        appendMapIndented(sb, blendTagInput);
        sb.append("Resolver input:\n");
        appendMapIndented(sb, blendResolverInput);
        sb.append("Precedence decisions:\n");
        for (Map.Entry<String, TagRuntimeBlend.Precedence> e : blendPrecedence.entrySet()) {
            sb.append("  ").append(e.getKey()).append(" -> ").append(e.getValue().name()).append("\n");
        }
        if (!blendDiscardedResolver.isEmpty()) {
            sb.append("Discarded resolver (tag precedence):\n");
            appendMapIndented(sb, blendDiscardedResolver);
        }
        sb.append("\n");

        sb.append("--- Final Result ---\n");
        appendMapIndented(sb, finalMergedBars);

        return sb.toString();
    }

    private static void appendMapIndented(StringBuilder sb, Map<String, Float> map) {
        if (map.isEmpty()) {
            sb.append("  (none)\n");
            return;
        }
        for (Map.Entry<String, Float> e : map.entrySet()) {
            sb.append("  ").append(e.getKey()).append(": ")
              .append(String.format(Locale.ROOT, "%.4f", e.getValue())).append("\n");
        }
    }
}
