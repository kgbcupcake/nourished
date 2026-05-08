package dev.maire.nourished.nutrition.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.maire.nourished.nutrition.Nourished;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates tag recommendation files for confident classifications.
 * Produces JSON entries ready to be added to tag files.
 */
public final class TagRecommendationWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private TagRecommendationWriter() {}

    /**
     * Write tag recommendations for high-confidence classifications.
     *
     * @param results Classification results (only confident ones will be included)
     * @param spreadThreshold The confidence spread threshold used
     */
    public static void writeRecommendations(
            List<ClassificationResult> results,
            float spreadThreshold
    ) throws IOException {
        List<ClassificationResult> confident = results.stream()
                .filter(r -> !r.uncertain())
                .filter(r -> r.confidenceSpread() >= spreadThreshold)
                .toList();

        if (confident.isEmpty()) {
            return;
        }

        Map<String, List<ClassificationResult>> byCategory = groupByDominant(confident);

        Path outputDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Files.createDirectories(outputDir);

        writeRecommendationsJson(byCategory, spreadThreshold, outputDir.resolve("tag_recommendations.json"));
        writeReadableRecommendations(byCategory, outputDir.resolve("tag_recommendations.txt"));
    }

    private static void writeRecommendationsJson(
            Map<String, List<ClassificationResult>> byCategory,
            float spreadThreshold,
            Path outputFile
    ) throws IOException {
        JsonObject root = new JsonObject();

        root.addProperty("generated", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        root.addProperty("confidence_threshold", spreadThreshold);
        root.addProperty("note", "Copy entries to data/nourished/tags/item/nutrients/<category>.json");

        JsonObject categories = new JsonObject();

        for (Map.Entry<String, List<ClassificationResult>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<ClassificationResult> items = entry.getValue();

            JsonObject categoryObj = new JsonObject();
            categoryObj.addProperty("target_file", "data/nourished/tags/item/nutrients/" + category + ".json");

            JsonArray tagEntries = new JsonArray();
            for (ClassificationResult r : items) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("id", r.itemId().toString());
                entryObj.addProperty("required", false);

                JsonObject meta = new JsonObject();
                meta.addProperty("confidence_spread", r.confidenceSpread());
                if (r.secondary() != null) {
                    meta.addProperty("secondary_category", r.secondary());
                }

                JsonArray topSignals = new JsonArray();
                for (ClassificationSignal sig : r.topSignals(3)) {
                    topSignals.add(sig.signalType() + ":" + sig.source());
                }
                meta.add("top_signals", topSignals);

                entryObj.add("_meta", meta);
                tagEntries.add(entryObj);
            }

            categoryObj.add("entries", tagEntries);

            JsonObject readyToCopy = new JsonObject();
            readyToCopy.addProperty("replace", false);
            JsonArray values = new JsonArray();
            for (ClassificationResult r : items) {
                JsonObject val = new JsonObject();
                val.addProperty("id", r.itemId().toString());
                val.addProperty("required", false);
                values.add(val);
            }
            readyToCopy.add("values", values);
            categoryObj.add("ready_to_copy", readyToCopy);

            categories.add(category, categoryObj);
        }

        root.add("categories", categories);

        JsonObject summary = new JsonObject();
        int totalItems = 0;
        for (List<ClassificationResult> list : byCategory.values()) {
            totalItems += list.size();
        }
        summary.addProperty("total_recommendations", totalItems);
        summary.addProperty("categories", byCategory.size());

        JsonObject perCategory = new JsonObject();
        for (Map.Entry<String, List<ClassificationResult>> e : byCategory.entrySet()) {
            perCategory.addProperty(e.getKey(), e.getValue().size());
        }
        summary.add("per_category", perCategory);
        root.add("summary", summary);

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            GSON.toJson(root, writer);
        }
    }

    private static void writeReadableRecommendations(
            Map<String, List<ClassificationResult>> byCategory,
            Path outputFile
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     NOURISHED TAG RECOMMENDATIONS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n\n");
            writer.write("Instructions:\n");
            writer.write("  Copy the entries below into the appropriate tag file:\n");
            writer.write("  data/nourished/tags/item/nutrients/<category>.json\n\n");

            for (Map.Entry<String, List<ClassificationResult>> entry : byCategory.entrySet()) {
                String category = entry.getKey();
                List<ClassificationResult> items = entry.getValue();

                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("  " + category.toUpperCase() + " (" + items.size() + " items)\n");
                writer.write("  Target: data/nourished/tags/item/nutrients/" + category + ".json\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n\n");

                writer.write("  JSON entries to add to \"values\" array:\n\n");

                for (ClassificationResult r : items) {
                    List<ClassificationSignal> topSignals = r.topSignals(3);
                    StringBuilder sigStr = new StringBuilder();
                    for (ClassificationSignal sig : topSignals) {
                        if (sigStr.length() > 0) sigStr.append(", ");
                        sigStr.append(sig.signalType());
                    }

                    writer.write(String.format("    // spread=%.1f, signals: %s\n",
                            r.confidenceSpread(), sigStr));
                    writer.write(String.format("    {\"id\": \"%s\", \"required\": false},\n\n",
                            r.itemId().toString()));
                }

                writer.write("\n");
            }

            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                           QUICK COPY BLOCKS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Ready-to-paste blocks for each category (without metadata comments):\n\n");

            for (Map.Entry<String, List<ClassificationResult>> entry : byCategory.entrySet()) {
                String category = entry.getKey();
                List<ClassificationResult> items = entry.getValue();

                writer.write("--- " + category + ".json ---\n");
                writer.write("{\n");
                writer.write("  \"replace\": false,\n");
                writer.write("  \"values\": [\n");

                for (int i = 0; i < items.size(); i++) {
                    ClassificationResult r = items.get(i);
                    String comma = (i < items.size() - 1) ? "," : "";
                    writer.write(String.format("    {\"id\": \"%s\", \"required\": false}%s\n",
                            r.itemId().toString(), comma));
                }

                writer.write("  ]\n");
                writer.write("}\n\n");
            }
        }
    }

    private static Map<String, List<ClassificationResult>> groupByDominant(List<ClassificationResult> results) {
        Map<String, List<ClassificationResult>> grouped = new LinkedHashMap<>();
        for (ClassificationResult r : results) {
            grouped.computeIfAbsent(r.dominant(), k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }
}
