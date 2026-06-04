package dev.maire.nourished.tooling.classification;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Formats a {@link ClassificationTrace} into the full NOURISHED INSPECTOR output.
 * Pure static, no state.
 */
public final class ClassificationTraceFormatter {

    private static final String SEP_FULL = "==================================================";
    private static final String SEP_HALF = "--------------------------------------------------";

    private ClassificationTraceFormatter() {}

    public static String format(ClassificationTrace trace, ItemStack stack) {
        StringBuilder sb = new StringBuilder();

        appendFull(sb, SEP_FULL);
        appendLine(sb, "NOURISHED INSPECTOR");
        appendFull(sb, SEP_FULL);
        appendLine(sb, "");

        appendItemSection(sb, trace, stack);
        appendFoodStatsSection(sb, stack);
        appendClassificationSummary(sb, trace);
        appendClassificationPath(sb, trace);
        appendInheritanceBreakdown(sb, trace);
        appendAggregation(sb, trace);
        appendWhyWon(sb, trace);
        appendDiagnostics(sb, trace);
        appendDeveloperMetadata(sb, trace);

        return sb.toString();
    }

    // ─── Sections ────────────────────────────────────────────────────────────────

    private static void appendItemSection(StringBuilder sb, ClassificationTrace trace, ItemStack stack) {
        appendLine(sb, "Item");
        appendLine(sb, SEP_HALF);
        String itemId = trace.itemId();
        String namespace = itemId.contains(":") ? itemId.substring(0, itemId.indexOf(':')) : itemId;
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        boolean edible = food != null && food.nutrition() > 0;
        appendKv(sb, "ID", itemId);
        appendKv(sb, "Namespace", namespace);
        appendKv(sb, "Edible", edible ? "YES" : "NO");
        appendLine(sb, "");
    }

    private static void appendFoodStatsSection(StringBuilder sb, ItemStack stack) {
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food == null) return;
        appendLine(sb, "Food Stats");
        appendLine(sb, SEP_HALF);
        appendKv(sb, "Nutrition", String.valueOf(food.nutrition()));
        appendKv(sb, "Saturation", String.format(Locale.ROOT, "%.2f", food.saturation()));
        appendLine(sb, "");
    }

    private static void appendClassificationSummary(StringBuilder sb, ClassificationTrace trace) {
        appendLine(sb, "Classification Summary");
        appendLine(sb, SEP_HALF);

        String dominant = trace.dominant();
        String status;
        if (trace.uncertain()) {
            status = "UNCERTAIN";
        } else if (dominant == null) {
            status = "UNCLASSIFIED";
        } else {
            status = "STABLE";
        }

        appendKv(sb, "Final Group", dominant != null ? dominant.toUpperCase(Locale.ROOT) : "NONE");

        // Confidence: derive from CONFIDENCE step detail
        String confidence = deriveConfidence(trace);
        appendKv(sb, "Confidence", confidence);

        appendKv(sb, "Pipeline", trace.pipeline().name());
        appendKv(sb, "Status", status);
        appendLine(sb, "");
    }

    private static void appendClassificationPath(StringBuilder sb, ClassificationTrace trace) {
        appendLine(sb, "Classification Path");
        appendLine(sb, SEP_HALF);
        appendLine(sb, "");

        List<ClassificationTraceStep> steps = trace.steps();
        for (int i = 0; i < steps.size(); i++) {
            ClassificationTraceStep step = steps.get(i);
            String icon = switch (step.status()) {
                case SUCCESS -> "✓";
                case FAILURE -> "✗";
                case WARNING -> "⚠";
                case SKIPPED -> "-";
            };
            appendLine(sb, "[" + (i + 1) + "] " + step.id().name() + " " + icon + " " + step.message());
        }
        appendLine(sb, "");
    }

    private static void appendInheritanceBreakdown(StringBuilder sb, ClassificationTrace trace) {
        List<ClassificationTraceStep> ingredientSteps = collectSteps(trace, TraceStepId.INGREDIENT_RESOLUTION);
        if (ingredientSteps.isEmpty()) return;

        appendLine(sb, "Inheritance Breakdown");
        appendLine(sb, SEP_HALF);

        for (ClassificationTraceStep step : ingredientSteps) {
            Map<String, Object> d = step.detail();
            String ingredientId = getString(d, "ingredientId", "unknown");
            String source = getString(d, "source", "NONE");
            appendLine(sb, ingredientId);

            @SuppressWarnings("unchecked")
            Map<String, Float> nutrients = d.get("nutrients") instanceof Map<?, ?> m
                    ? (Map<String, Float>) m : null;

            if (step.status() == TraceStepStatus.SUCCESS && nutrients != null && !nutrients.isEmpty()) {
                appendLine(sb, "  Source: " + source);
                String result = String.join(", ", nutrients.keySet()).toUpperCase(Locale.ROOT);
                appendLine(sb, "  Result: " + result);
            } else if (step.status() == TraceStepStatus.SKIPPED) {
                String skipReason = getString(d, "skipReason", "skipped");
                appendLine(sb, "  Source: " + source);
                appendLine(sb, "  Result: SKIPPED");
                appendLine(sb, "  Reason: " + skipReason);
            } else {
                appendLine(sb, "  Source: NONE");
                appendLine(sb, "  Result: UNCLASSIFIED");
                String failure = "No direct tags · No runtime match · No recipe ingredients";
                appendLine(sb, "  Failure: " + failure);
            }
            appendLine(sb, "");
        }
    }

    private static void appendAggregation(StringBuilder sb, ClassificationTrace trace) {
        List<ClassificationTraceStep> aggSteps = collectSteps(trace, TraceStepId.SIGNAL_AGGREGATION);
        if (aggSteps.isEmpty()) return;

        appendLine(sb, "Aggregation");
        appendLine(sb, SEP_HALF);

        // Collect ingredient contributions for "Raw Contributions"
        List<ClassificationTraceStep> ingredientSteps = collectSteps(trace, TraceStepId.INGREDIENT_RESOLUTION);
        if (!ingredientSteps.isEmpty()) {
            appendLine(sb, "Raw Contributions");
            for (ClassificationTraceStep step : ingredientSteps) {
                Map<String, Object> d = step.detail();
                String ingredientId = getString(d, "ingredientId", "unknown");
                String shortId = ingredientId.contains(":") ? ingredientId.substring(ingredientId.indexOf(':') + 1) : ingredientId;
                @SuppressWarnings("unchecked")
                Map<String, Float> nutrients = d.get("nutrients") instanceof Map<?, ?> m
                        ? (Map<String, Float>) m : null;
                if (nutrients != null) {
                    for (Map.Entry<String, Float> e : nutrients.entrySet()) {
                        appendLine(sb, "  " + e.getKey() + ": " + shortId + " = " + String.format(Locale.ROOT, "%.1f", e.getValue()));
                    }
                }
            }
            appendLine(sb, "");
        }

        ClassificationTraceStep aggStep = aggSteps.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Float> merged = aggStep.detail().get("mergedScores") instanceof Map<?, ?> m
                ? (Map<String, Float>) m : null;
        if (merged != null && !merged.isEmpty()) {
            float total = merged.values().stream().reduce(0f, Float::sum);
            appendLine(sb, "Weighted Totals");
            for (Map.Entry<String, Float> e : merged.entrySet()) {
                float pct = total > 0 ? e.getValue() / total * 100f : 0f;
                appendLine(sb, String.format(Locale.ROOT, "  %-12s %.1f%%", e.getKey(), pct));
            }
        }
        appendLine(sb, "");
    }

    private static void appendWhyWon(StringBuilder sb, ClassificationTrace trace) {
        String dominant = trace.dominant();
        if (dominant == null) return;

        List<ClassificationTraceStep> aggSteps = collectSteps(trace, TraceStepId.SIGNAL_AGGREGATION);
        if (aggSteps.isEmpty()) return;

        appendLine(sb, "Why " + dominant.toUpperCase(Locale.ROOT) + " Won");
        appendLine(sb, SEP_HALF);

        @SuppressWarnings("unchecked")
        Map<String, Float> merged = aggSteps.get(0).detail().get("mergedScores") instanceof Map<?, ?> m
                ? (Map<String, Float>) m : null;

        if (merged != null) {
            float topScore = merged.getOrDefault(dominant, 0f);
            float secondScore = 0f;
            for (Map.Entry<String, Float> e : merged.entrySet()) {
                if (!e.getKey().equals(dominant) && e.getValue() > secondScore) {
                    secondScore = e.getValue();
                }
            }
            float diff = topScore - secondScore;
            appendLine(sb, "  " + dominant + " score: " + String.format(Locale.ROOT, "%.1f", topScore));
            appendLine(sb, "  secondary score: " + String.format(Locale.ROOT, "%.1f", secondScore));
            appendLine(sb, "  Difference: +" + String.format(Locale.ROOT, "%.1f", diff));
            appendLine(sb, "  Confidence: " + deriveConfidence(trace));
            appendLine(sb, "  Reason: " + (trace.summaryReason().isEmpty() ? "Highest weighted contribution." : trace.summaryReason()));
        }
        appendLine(sb, "");
    }

    private static void appendDiagnostics(StringBuilder sb, ClassificationTrace trace) {
        List<DiagnosticEntry> diagnostics = collectDiagnostics(trace);

        long errorCount = diagnostics.stream().filter(d -> d.isError).count();
        long warnCount = diagnostics.stream().filter(d -> !d.isError).count();
        long infoCount = trace.steps().stream().filter(s -> s.status() == TraceStepStatus.SUCCESS).count();

        appendLine(sb, "Diagnostics");
        appendLine(sb, SEP_HALF);
        appendKv(sb, "Errors", String.valueOf(errorCount));
        appendKv(sb, "Warnings", String.valueOf(warnCount));
        appendKv(sb, "Infos", String.valueOf(infoCount));

        if (!diagnostics.isEmpty()) {
            appendLine(sb, "");
            for (DiagnosticEntry entry : diagnostics) {
                appendLine(sb, (entry.isError ? "ERROR " : "WARNING ") + entry.code);
                appendLine(sb, "  " + entry.summary);
                appendLine(sb, "  Cause: " + entry.cause);
                appendLine(sb, "  Impact: " + entry.impact);
                if (entry.fix != null) {
                    appendLine(sb, "  Suggested Fix: " + entry.fix);
                }
                appendLine(sb, "");
            }
        }
    }

    private static void appendDeveloperMetadata(StringBuilder sb, ClassificationTrace trace) {
        List<ClassificationTraceStep> cacheSteps = collectSteps(trace, TraceStepId.RESOLVER_CACHE);
        String cacheStatus = "MISS";
        if (!cacheSteps.isEmpty()) {
            Object hit = cacheSteps.get(0).detail().get("hit");
            if (Boolean.TRUE.equals(hit)) {
                cacheStatus = "HIT";
            }
        }

        // Trace ID: first 8 chars of UUID derived from itemId + step count
        String traceSource = trace.itemId() + trace.steps().size();
        String traceId = UUID.nameUUIDFromBytes(traceSource.getBytes()).toString().substring(0, 8);

        appendLine(sb, "Developer Metadata");
        appendLine(sb, SEP_HALF);
        appendKv(sb, "Pipeline", trace.pipeline().name());
        appendKv(sb, "Trace ID", traceId);
        appendKv(sb, "Steps", String.valueOf(trace.steps().size()));
        appendKv(sb, "Uncertain", String.valueOf(trace.uncertain()));
        appendKv(sb, "Cache", cacheStatus);
    }

    // ─── Diagnostics collection ───────────────────────────────────────────────

    private static List<DiagnosticEntry> collectDiagnostics(ClassificationTrace trace) {
        List<DiagnosticEntry> list = new ArrayList<>();

        for (ClassificationTraceStep step : trace.steps()) {
            Map<String, Object> d = step.detail();

            if (step.id() == TraceStepId.INGREDIENT_RESOLUTION
                    && (step.status() == TraceStepStatus.FAILURE || step.status() == TraceStepStatus.WARNING)) {
                String ingredientId = getString(d, "ingredientId", "unknown");
                list.add(new DiagnosticEntry(
                        false,
                        "NRS-W001",
                        ingredientId + " is currently unclassified.",
                        "No nutrient source found for ingredient.",
                        "Ingredient ignored during inheritance.",
                        "Add to nourished:nutrients/vegetables tag\n                 OR create a food_classifications datapack entry."
                ));
            }

            if (step.id() == TraceStepId.CONFIDENCE && step.status() == TraceStepStatus.WARNING) {
                list.add(new DiagnosticEntry(
                        false,
                        "NRS-W002",
                        "Classification confidence is below threshold.",
                        "Signal spread below spread threshold.",
                        "Classification marked as UNCERTAIN.",
                        null
                ));
            }

            if (step.id() == TraceStepId.HARD_FALLBACK && step.status() == TraceStepStatus.FAILURE) {
                list.add(new DiagnosticEntry(
                        true,
                        "NRS-001",
                        "Item could not be classified through any pipeline path.",
                        "No signal source produced a valid classification.",
                        "Item will appear as unclassified in diet tracking.",
                        "Add to a nourished:nutrients/* tag or create a datapack entry."
                ));
            }

            if (step.id() == TraceStepId.RECIPE_LOOKUP && step.status() == TraceStepStatus.FAILURE) {
                list.add(new DiagnosticEntry(
                        true,
                        "NRS-002",
                        "No recipe found for this item.",
                        "Item has no known crafting recipe on the server.",
                        "Recipe inheritance path unavailable.",
                        null
                ));
            }

            if (step.id() == TraceStepId.PRIMARY_RECIPE_MERGE && step.status() == TraceStepStatus.FAILURE) {
                list.add(new DiagnosticEntry(
                        true,
                        "NRS-003",
                        "No classified ingredients found in recipe.",
                        "All recipe ingredients are unclassified.",
                        "Recipe inheritance produced no nutrient keys.",
                        "Classify at least one recipe ingredient via nutrient tags."
                ));
            }
        }

        return list;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static String deriveConfidence(ClassificationTrace trace) {
        for (ClassificationTraceStep step : trace.steps()) {
            if (step.id() == TraceStepId.CONFIDENCE) {
                Object cs = step.detail().get("confidenceScore");
                if (cs instanceof Number n) {
                    return String.format(Locale.ROOT, "%.0f%%", n.doubleValue() * 100.0);
                }
                // Fallback: compute from spread/threshold
                Object spread = step.detail().get("spread");
                Object threshold = step.detail().get("threshold");
                if (spread instanceof Number sp && threshold instanceof Number th) {
                    double ratio = th.doubleValue() > 0 ? sp.doubleValue() / (th.doubleValue() * 2) : 0;
                    ratio = Math.max(0.0, Math.min(1.0, ratio));
                    return String.format(Locale.ROOT, "%.0f%%", ratio * 100.0);
                }
            }
        }
        // Fallback from KEYWORD_SUFFIX_SCORING spread
        for (ClassificationTraceStep step : trace.steps()) {
            if (step.id() == TraceStepId.KEYWORD_SUFFIX_SCORING) {
                Object spread = step.detail().get("spread");
                Object threshold = step.detail().get("spreadThreshold");
                if (spread instanceof Number sp && threshold instanceof Number th) {
                    double ratio = th.doubleValue() > 0 ? sp.doubleValue() / (th.doubleValue() * 2) : 0;
                    ratio = Math.max(0.0, Math.min(1.0, ratio));
                    return String.format(Locale.ROOT, "%.0f%%", ratio * 100.0);
                }
            }
        }
        return "N/A";
    }

    private static List<ClassificationTraceStep> collectSteps(ClassificationTrace trace, TraceStepId id) {
        List<ClassificationTraceStep> result = new ArrayList<>();
        for (ClassificationTraceStep step : trace.steps()) {
            if (step.id() == id) result.add(step);
        }
        return result;
    }

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object v = map.get(key);
        return v != null ? v.toString() : fallback;
    }

    private static void appendLine(StringBuilder sb, String line) {
        sb.append(line).append('\n');
    }

    private static void appendFull(StringBuilder sb, String line) {
        sb.append(line).append('\n');
    }

    private static void appendKv(StringBuilder sb, String label, String value) {
        sb.append(label).append(": ").append(value).append('\n');
    }

    // ─── Diagnostic entry ─────────────────────────────────────────────────────

    private record DiagnosticEntry(
            boolean isError,
            String code,
            String summary,
            String cause,
            String impact,
            @Nullable String fix
    ) {}
}
