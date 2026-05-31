package dev.maire.nourished.tooling.scanner.analysis;

import dev.maire.nourished.api.ApiStatus;

/**
 * Aggregate quality metrics for a multi-nutrient analysis run.
 *
 * @param total                  Total foods analyzed
 * @param singleNutrient         Foods with only a dominant nutrient (no qualifying secondaries)
 * @param multiNutrient          Foods with at least one qualifying secondary nutrient
 * @param ambiguous              Foods flagged as ambiguous (dominant/second spread too small)
 * @param averageSecondaryCount  Average qualifying secondary count per multi-nutrient food
 */
@ApiStatus.Internal
public record ScannerMetrics(
        int total,
        int singleNutrient,
        int multiNutrient,
        int ambiguous,
        double averageSecondaryCount
) {}
