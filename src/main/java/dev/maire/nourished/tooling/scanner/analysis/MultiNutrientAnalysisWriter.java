package dev.maire.nourished.tooling.scanner.analysis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists {@link MultiNutrientAnalysisResult} outputs under {@code config/<MODID>/scanner_analysis/}.
 */
@ApiStatus.Internal
final class MultiNutrientAnalysisWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String OUTPUT_SUBDIR = "scanner_analysis";
    private static final String DATAPACK_SUBDIR = "generated-multi-nutrient-datapack";

    private MultiNutrientAnalysisWriter() {}

    static Path resolveOutputDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve(OUTPUT_SUBDIR);
    }

    static void writeAll(
            MultiNutrientAnalysisResult result,
            Path outputDir,
            @Nullable Float absoluteThreshold,
            @Nullable Float relativeThreshold,
            @Nullable Float ambiguityThreshold
    ) {
        writeSafely(outputDir.resolve("multi_nutrient_recommendations.json"), () ->
                writeRecommendationsJson(result, outputDir.resolve("multi_nutrient_recommendations.json"),
                        absoluteThreshold, relativeThreshold, ambiguityThreshold));

        writeSafely(outputDir.resolve("multi_nutrient_recommendations.txt"), () ->
                writeRecommendationsTxt(result, outputDir.resolve("multi_nutrient_recommendations.txt")));

        writeSafely(outputDir.resolve("ambiguous_foods.txt"), () ->
                writeAmbiguousFoodsTxt(result, outputDir.resolve("ambiguous_foods.txt")));

        writeSafely(outputDir.resolve("nutrient_overlap_matrix.txt"), () ->
                writeOverlapMatrixTxt(result, outputDir.resolve("nutrient_overlap_matrix.txt")));

        writeSafely(outputDir.resolve("scanner_metrics.txt"), () ->
                writeMetricsTxt(result, outputDir.resolve("scanner_metrics.txt")));

        writeSafely(outputDir.resolve(DATAPACK_SUBDIR), () ->
                writeDatapack(result, outputDir.resolve(DATAPACK_SUBDIR)));

        ScannerMetrics m = result.metrics();
        Nourished.LOGGER.info(
                "[MultiNutrientAnalysisPipeline] Wrote analysis: {} foods, {} multi-nutrient, {} ambiguous → {}",
                m.total(), m.multiNutrient(), m.ambiguous(), outputDir.toAbsolutePath());
    }

    private static void writeSafely(Path target, WriteAction action) {
        try {
            action.run();
        } catch (IOException e) {
            Nourished.LOGGER.error(
                    "[MultiNutrientAnalysisPipeline] Failed to write {}", target.getFileName(), e);
        }
    }

    @FunctionalInterface
    private interface WriteAction {
        void run() throws IOException;
    }

    private static void writeRecommendationsJson(
            MultiNutrientAnalysisResult result,
            Path outputFile,
            @Nullable Float absoluteThreshold,
            @Nullable Float relativeThreshold,
            @Nullable Float ambiguityThreshold
    ) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("generated", LocalDateTime.now().format(TIMESTAMP_FORMAT));

        if (absoluteThreshold != null && relativeThreshold != null && ambiguityThreshold != null) {
            JsonObject thresholds = new JsonObject();
            thresholds.addProperty("absolute", absoluteThreshold);
            thresholds.addProperty("relative", relativeThreshold);
            thresholds.addProperty("ambiguity", ambiguityThreshold);
            root.add("thresholds", thresholds);
        }

        root.addProperty("note", "Secondary nutrient tag recommendations for multi-nutrient foods. "
                + "Copy entries to data/" + Nourished.MODID + "/tags/item/nutrients/<category>.json");

        JsonObject categories = new JsonObject();
        for (Map.Entry<String, List<MultiNutrientEntry>> entry : result.secondaryByNutrient().entrySet()) {
            JsonObject categoryObj = new JsonObject();
            categoryObj.addProperty("target_file",
                    "data/" + Nourished.MODID + "/tags/item/nutrients/" + entry.getKey() + ".json");

            JsonArray entries = new JsonArray();
            for (MultiNutrientEntry e : entry.getValue()) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("id", e.itemId().toString());
                entryObj.addProperty("score", e.score());
                entryObj.addProperty("dominant", e.dominant());
                entries.add(entryObj);
            }
            categoryObj.add("entries", entries);
            categories.add(entry.getKey(), categoryObj);
        }
        root.add("categories", categories);

        JsonObject summary = buildSummaryJson(result.metrics());
        root.add("summary", summary);

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            GSON.toJson(root, writer);
        }
    }

    private static JsonObject buildSummaryJson(ScannerMetrics metrics) {
        JsonObject summary = new JsonObject();
        summary.addProperty("total", metrics.total());
        summary.addProperty("single_nutrient", metrics.singleNutrient());
        summary.addProperty("multi_nutrient", metrics.multiNutrient());
        summary.addProperty("ambiguous", metrics.ambiguous());
        summary.addProperty("average_secondary_count", metrics.averageSecondaryCount());
        return summary;
    }

    private static void writeRecommendationsTxt(
            MultiNutrientAnalysisResult result,
            Path outputFile
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                  MULTI-NUTRIENT TAG RECOMMENDATIONS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n\n");
            writer.write("Instructions:\n");
            writer.write("  Add items below to their secondary nutrient tag file:\n");
            writer.write("  data/" + Nourished.MODID + "/tags/item/nutrients/<category>.json\n\n");

            if (result.secondaryByNutrient().isEmpty()) {
                writer.write("  (no multi-nutrient recommendations)\n");
            }

            for (Map.Entry<String, List<MultiNutrientEntry>> entry : result.secondaryByNutrient().entrySet()) {
                String nutrient = entry.getKey();
                List<MultiNutrientEntry> items = entry.getValue();

                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("  SECONDARY: " + nutrient.toUpperCase() + " (" + items.size() + " items)\n");
                writer.write("  Target: data/" + Nourished.MODID + "/tags/item/nutrients/" + nutrient + ".json\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n\n");

                for (MultiNutrientEntry e : items) {
                    writer.write(String.format("    %s  score=%.3f  dominant=%s\n",
                            e.itemId(), e.score(), e.dominant()));
                }
                writer.write("\n");
            }

            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                           QUICK COPY BLOCKS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");

            for (Map.Entry<String, List<MultiNutrientEntry>> entry : result.secondaryByNutrient().entrySet()) {
                String nutrient = entry.getKey();
                List<MultiNutrientEntry> items = entry.getValue();

                writer.write("--- " + nutrient + ".json ---\n");
                writer.write("{\n");
                writer.write("  \"replace\": false,\n");
                writer.write("  \"values\": [\n");

                for (int i = 0; i < items.size(); i++) {
                    MultiNutrientEntry e = items.get(i);
                    String comma = (i < items.size() - 1) ? "," : "";
                    writer.write(String.format("    {\"id\": \"%s\", \"required\": false}%s\n",
                            e.itemId(), comma));
                }

                writer.write("  ]\n");
                writer.write("}\n\n");
            }
        }
    }

    private static void writeAmbiguousFoodsTxt(
            MultiNutrientAnalysisResult result,
            Path outputFile
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     AMBIGUOUS FOODS — MANUAL REVIEW\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("Count: " + result.ambiguousFoods().size() + "\n\n");

            if (result.ambiguousFoods().isEmpty()) {
                writer.write("  (no ambiguous foods)\n");
                return;
            }

            for (AmbiguousFoodEntry entry : result.ambiguousFoods()) {
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("  " + entry.itemId() + "\n");
                writer.write("  spread=" + String.format("%.3f", entry.spread()) + "\n");
                writer.write("  *** MANUAL REVIEW ***\n\n");
                writer.write("  Scores:\n");

                List<Map.Entry<String, Float>> sorted = entry.scores().entrySet().stream()
                        .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                        .toList();
                for (Map.Entry<String, Float> score : sorted) {
                    writer.write(String.format("    %-12s %.3f\n", score.getKey(), score.getValue()));
                }
                writer.write("\n");
            }
        }
    }

    private static void writeOverlapMatrixTxt(
            MultiNutrientAnalysisResult result,
            Path outputFile
    ) throws IOException {
        NutrientOverlapMatrix matrix = result.overlapMatrix();
        List<String> nutrients = matrix.nutrients();

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     NUTRIENT OVERLAP MATRIX\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("Co-occurrence counts for nutrient pairs in multi-nutrient foods.\n\n");

            if (nutrients.isEmpty()) {
                writer.write("  (no co-occurrence data)\n");
                return;
            }

            int colWidth = Math.max(10, nutrients.stream().mapToInt(String::length).max().orElse(8) + 2);
            String headerFormat = "%-" + colWidth + "s";

            writer.write(String.format(headerFormat, ""));
            for (String col : nutrients) {
                writer.write(String.format(headerFormat, col));
            }
            writer.write("\n");

            for (String row : nutrients) {
                writer.write(String.format(headerFormat, row));
                Map<String, Integer> rowData = matrix.matrix().getOrDefault(row, Map.of());
                for (String col : nutrients) {
                    writer.write(String.format(headerFormat, rowData.getOrDefault(col, 0)));
                }
                writer.write("\n");
            }
        }
    }

    private static void writeMetricsTxt(
            MultiNutrientAnalysisResult result,
            Path outputFile
    ) throws IOException {
        ScannerMetrics m = result.metrics();

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     SCANNER ANALYSIS METRICS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n\n");
            writer.write(String.format("  Total foods analyzed:       %d\n", m.total()));
            writer.write(String.format("  Single-nutrient:            %d\n", m.singleNutrient()));
            writer.write(String.format("  Multi-nutrient:             %d\n", m.multiNutrient()));
            writer.write(String.format("  Ambiguous (manual review):  %d\n", m.ambiguous()));
            writer.write(String.format("  Avg secondary count:        %.2f\n", m.averageSecondaryCount()));
            writer.write("\n");

            if (m.total() > 0) {
                double multiPct = 100.0 * m.multiNutrient() / m.total();
                double ambPct = 100.0 * m.ambiguous() / m.total();
                writer.write(String.format("  Multi-nutrient rate:        %.1f%%\n", multiPct));
                writer.write(String.format("  Ambiguity rate:             %.1f%%\n", ambPct));
            }
        }
    }

    private static void writeDatapack(
            MultiNutrientAnalysisResult result,
            Path datapackRoot
    ) throws IOException {
        Files.createDirectories(datapackRoot);

        Path packMeta = datapackRoot.resolve("pack.mcmeta");
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", Nourished.MODID + " auto-generated multi-nutrient tags");
        root.add("pack", pack);
        try (Writer w = Files.newBufferedWriter(packMeta)) {
            GSON.toJson(root, w);
        }

        Path tagsDir = datapackRoot
                .resolve("data")
                .resolve(Nourished.MODID)
                .resolve("tags")
                .resolve("item")
                .resolve("nutrients");
        Files.createDirectories(tagsDir);

        for (Map.Entry<String, List<MultiNutrientEntry>> entry : result.secondaryByNutrient().entrySet()) {
            String nutrient = entry.getKey();
            List<MultiNutrientEntry> items = entry.getValue();

            Map<String, MultiNutrientEntry> uniqueById = new LinkedHashMap<>();
            for (MultiNutrientEntry e : items) {
                uniqueById.putIfAbsent(e.itemId().toString(), e);
            }

            Path tagFile = tagsDir.resolve(nutrient + ".json");
            JsonObject tagObj = new JsonObject();
            tagObj.addProperty("replace", false);
            JsonArray values = new JsonArray();
            List<String> sortedIds = new ArrayList<>(uniqueById.keySet());
            Collections.sort(sortedIds);
            for (String id : sortedIds) {
                JsonObject val = new JsonObject();
                val.addProperty("id", id);
                val.addProperty("required", false);
                values.add(val);
            }
            tagObj.add("values", values);

            try (Writer w = Files.newBufferedWriter(tagFile)) {
                GSON.toJson(tagObj, w);
            }
        }
    }
}
