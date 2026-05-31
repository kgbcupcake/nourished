package dev.maire.nourished.tooling.scanner.analysis;

import dev.maire.nourished.api.ApiStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Complete result of a multi-nutrient analysis pass.
 *
 * @param secondaryByNutrient Items grouped by secondary nutrient, each list sorted score DESC then itemId ASC
 * @param ambiguousFoods      Foods flagged for manual review (excluded from recommendations)
 * @param overlapMatrix       Co-occurrence counts for nutrient pairs
 * @param metrics             Aggregate quality metrics
 */
@ApiStatus.Internal
public record MultiNutrientAnalysisResult(
        Map<String, List<MultiNutrientEntry>> secondaryByNutrient,
        List<AmbiguousFoodEntry> ambiguousFoods,
        NutrientOverlapMatrix overlapMatrix,
        ScannerMetrics metrics
) {
    private static final Comparator<MultiNutrientEntry> ENTRY_ORDER = Comparator
            .comparing(MultiNutrientEntry::score, Comparator.reverseOrder())
            .thenComparing(e -> e.itemId().toString());

    public MultiNutrientAnalysisResult {
        Map<String, List<MultiNutrientEntry>> sorted = new TreeMap<>();
        for (Map.Entry<String, List<MultiNutrientEntry>> entry : secondaryByNutrient.entrySet()) {
            List<MultiNutrientEntry> list = new ArrayList<>(entry.getValue());
            list.sort(ENTRY_ORDER);
            sorted.put(entry.getKey(), List.copyOf(list));
        }
        secondaryByNutrient = Map.copyOf(sorted);
        ambiguousFoods = List.copyOf(ambiguousFoods);
    }
}
