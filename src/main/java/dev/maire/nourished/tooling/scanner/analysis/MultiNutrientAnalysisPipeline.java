package dev.maire.nourished.tooling.scanner.analysis;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.ClassifiedFoodCollector;
import dev.maire.nourished.tooling.scanner.ClassificationResult;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates multi-nutrient analysis, aggregation, and persistence.
 *
 * <p>Analysis ({@link #analyze}) is pure and reusable by commands, GUIs, tests, and KubeJS.
 * Persistence ({@link #write}) is fault-tolerant — a failure writing one output does not
 * prevent remaining outputs from being written.</p>
 */
@ApiStatus.Internal
public final class MultiNutrientAnalysisPipeline {

    private MultiNutrientAnalysisPipeline() {}

    /**
     * Analyzes classification results for multi-nutrient patterns.
     *
     * @param results            Classification results from the food scanner
     * @param absoluteThreshold  Minimum score for a secondary nutrient to qualify
     * @param relativeThreshold  Secondary score must be at least dominantScore * this value
     * @param ambiguityThreshold Foods where dominant − second &lt; this are flagged ambiguous
     */
    public static MultiNutrientAnalysisResult analyze(
            List<ClassificationResult> results,
            float absoluteThreshold,
            float relativeThreshold,
            float ambiguityThreshold
    ) {
        Map<String, List<MultiNutrientEntry>> secondaryByNutrient = new HashMap<>();
        List<AmbiguousFoodEntry> ambiguousFoods = new ArrayList<>();
        Map<String, Map<String, Integer>> rawPairCounts = new HashMap<>();

        int total = 0;
        int singleNutrient = 0;
        int multiNutrient = 0;
        int ambiguous = 0;
        int totalSecondaries = 0;

        for (ClassificationResult r : results) {
            Map<String, Float> scores = r.scores();
            if (scores == null || scores.isEmpty()) {
                continue;
            }

            total++;

            String dominant = resolveDominant(scores);
            float dominantScore = scores.getOrDefault(dominant, 0f);
            float secondScore = resolveSecondScore(scores, dominant);
            float spread = dominantScore - secondScore;

            if (spread < ambiguityThreshold) {
                ambiguous++;
                ambiguousFoods.add(new AmbiguousFoodEntry(r.itemId(), scores, spread));
                continue;
            }

            List<String> qualifyingSecondaries = resolveQualifyingSecondaries(
                    scores, dominant, dominantScore, absoluteThreshold, relativeThreshold);

            if (qualifyingSecondaries.isEmpty()) {
                singleNutrient++;
            } else {
                multiNutrient++;
                totalSecondaries += qualifyingSecondaries.size();

                for (String secondaryKey : qualifyingSecondaries) {
                    float secondaryScore = scores.getOrDefault(secondaryKey, 0f);
                    secondaryByNutrient
                            .computeIfAbsent(secondaryKey, k -> new ArrayList<>())
                            .add(new MultiNutrientEntry(r.itemId(), secondaryScore, dominant));
                }

                Set<String> nutrientSet = new HashSet<>(qualifyingSecondaries);
                nutrientSet.add(dominant);
                incrementPairCounts(rawPairCounts, nutrientSet);
            }
        }

        double averageSecondaryCount = multiNutrient > 0
                ? (double) totalSecondaries / multiNutrient
                : 0.0;

        ScannerMetrics metrics = new ScannerMetrics(
                total, singleNutrient, multiNutrient, ambiguous, averageSecondaryCount);

        NutrientOverlapMatrix overlapMatrix = NutrientOverlapMatrix.fromPairCounts(rawPairCounts);

        return new MultiNutrientAnalysisResult(
                secondaryByNutrient, ambiguousFoods, overlapMatrix, metrics);
    }

    /**
     * Writes all analysis outputs to {@code config/<MODID>/scanner_analysis/}.
     * Threshold metadata is omitted; use {@link #run} for full JSON output.
     */
    public static void write(MultiNutrientAnalysisResult result) throws IOException {
        Path outputDir = MultiNutrientAnalysisWriter.resolveOutputDir();
        Files.createDirectories(outputDir);
        MultiNutrientAnalysisWriter.writeAll(result, outputDir, null, null, null);
    }

    /**
     * Analyzes and writes all outputs in one step.
     */
    public static void run(
            List<ClassificationResult> results,
            float absoluteThreshold,
            float relativeThreshold,
            float ambiguityThreshold
    ) {
        MultiNutrientAnalysisResult result = analyze(
                results, absoluteThreshold, relativeThreshold, ambiguityThreshold);
        try {
            Path outputDir = MultiNutrientAnalysisWriter.resolveOutputDir();
            Files.createDirectories(outputDir);
            MultiNutrientAnalysisWriter.writeAll(
                    result, outputDir, absoluteThreshold, relativeThreshold, ambiguityThreshold);
        } catch (IOException e) {
            Nourished.LOGGER.error("[MultiNutrientAnalysisPipeline] Failed to create output directory", e);
        }
    }

    /**
     * Analyzes every classified food in {@link dev.maire.nourished.core.nutrition.FoodNutritionRegistry}
     * (datapack tags, API, and scanner cache) and writes outputs to {@code config/<MODID>/scanner_analysis/}.
     */
    public static void runFullRegistry(
            float absoluteThreshold,
            float relativeThreshold,
            float ambiguityThreshold
    ) {
        Map<ResourceLocation, Map<String, Float>> registryFoods =
                ClassifiedFoodCollector.collectAllClassifiedFoodScores();
        if (registryFoods == null || registryFoods.isEmpty()) {
            Nourished.LOGGER.warn(
                    "[MultiNutrientAnalysisPipeline] Registry empty or unavailable — skipping full registry analysis");
            return;
        }

        Nourished.LOGGER.info(
                "[MultiNutrientAnalysisPipeline] Pulled {} classified foods from registry",
                registryFoods.size());

        List<ClassificationResult> results = new ArrayList<>(registryFoods.size());
        for (Map.Entry<ResourceLocation, Map<String, Float>> entry : registryFoods.entrySet()) {
            results.add(fromRegistryScores(entry.getKey(), entry.getValue()));
        }

        MultiNutrientAnalysisResult result = analyze(
                results, absoluteThreshold, relativeThreshold, ambiguityThreshold);
        try {
            write(result);
        } catch (IOException e) {
            Nourished.LOGGER.error("[MultiNutrientAnalysisPipeline] Failed to write full registry analysis", e);
        }
    }

    private static ClassificationResult fromRegistryScores(
            ResourceLocation itemId,
            Map<String, Float> scores
    ) {
        List<Map.Entry<String, Float>> sorted = scores.entrySet().stream()
                .sorted(Comparator
                        .comparing(Map.Entry<String, Float>::getValue, Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .toList();

        if (sorted.isEmpty()) {
            return ClassificationResult.empty(itemId, "");
        }

        String dominant = sorted.get(0).getKey();
        String secondary = sorted.size() > 1 ? sorted.get(1).getKey() : null;
        float topScore = sorted.get(0).getValue();
        float secondScore = sorted.size() > 1 ? sorted.get(1).getValue() : 0f;
        float spread = topScore - secondScore;

        return new ClassificationResult(
                itemId,
                scores,
                dominant,
                secondary,
                spread,
                List.of(),
                false
        );
    }

    private static String resolveDominant(Map<String, Float> scores) {
        return scores.entrySet().stream()
                .sorted(Comparator
                        .comparing(Map.Entry<String, Float>::getValue, Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    private static float resolveSecondScore(Map<String, Float> scores, String dominant) {
        return scores.entrySet().stream()
                .filter(e -> !e.getKey().equals(dominant))
                .map(Map.Entry::getValue)
                .max(Float::compare)
                .orElse(0f);
    }

    private static List<String> resolveQualifyingSecondaries(
            Map<String, Float> scores,
            String dominant,
            float dominantScore,
            float absoluteThreshold,
            float relativeThreshold
    ) {
        float relativeMin = dominantScore * relativeThreshold;
        List<String> secondaries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            String key = entry.getKey();
            float score = entry.getValue();
            if (key.equals(dominant)) {
                continue;
            }
            if (score >= absoluteThreshold && score >= relativeMin) {
                secondaries.add(key);
            }
        }
        Collections.sort(secondaries);
        return secondaries;
    }

    private static void incrementPairCounts(
            Map<String, Map<String, Integer>> rawPairCounts,
            Set<String> nutrients
    ) {
        List<String> sorted = new ArrayList<>(nutrients);
        Collections.sort(sorted);
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                String a = sorted.get(i);
                String b = sorted.get(j);
                rawPairCounts
                        .computeIfAbsent(a, k -> new HashMap<>())
                        .merge(b, 1, Integer::sum);
                rawPairCounts
                        .computeIfAbsent(b, k -> new HashMap<>())
                        .merge(a, 1, Integer::sum);
            }
        }
    }
}
