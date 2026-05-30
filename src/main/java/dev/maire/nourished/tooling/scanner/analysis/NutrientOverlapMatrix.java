package dev.maire.nourished.tooling.scanner.analysis;

import dev.maire.nourished.api.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Symmetric co-occurrence matrix of nutrient pairs in multi-nutrient foods.
 * Row and column keys are sorted alphabetically.
 *
 * @param matrix nutrient → (nutrient → co-occurrence count)
 */
@ApiStatus.Internal
public record NutrientOverlapMatrix(
        Map<String, Map<String, Integer>> matrix
) {
    public NutrientOverlapMatrix {
        matrix = Map.copyOf(matrix);
    }

    /**
     * Builds a sorted symmetric matrix from raw pair counts.
     * Keys in each row are sorted alphabetically; rows are sorted alphabetically.
     */
    public static NutrientOverlapMatrix fromPairCounts(Map<String, Map<String, Integer>> rawCounts) {
        List<String> nutrients = new ArrayList<>(rawCounts.keySet());
        Collections.sort(nutrients);

        Map<String, Map<String, Integer>> sorted = new LinkedHashMap<>();
        for (String row : nutrients) {
            Map<String, Integer> rowData = rawCounts.getOrDefault(row, Map.of());
            Map<String, Integer> sortedRow = new TreeMap<>();
            for (String col : nutrients) {
                int count = 0;
                if (row.equals(col)) {
                    count = rowData.getOrDefault(col, 0);
                } else {
                    count = rowData.getOrDefault(col, 0);
                    if (count == 0) {
                        Map<String, Integer> reverse = rawCounts.get(col);
                        if (reverse != null) {
                            count = reverse.getOrDefault(row, 0);
                        }
                    }
                }
                sortedRow.put(col, count);
            }
            sorted.put(row, Collections.unmodifiableMap(sortedRow));
        }
        return new NutrientOverlapMatrix(Collections.unmodifiableMap(sorted));
    }

    /**
     * Returns all nutrient keys in alphabetical order.
     */
    public List<String> nutrients() {
        return List.copyOf(matrix.keySet());
    }
}
