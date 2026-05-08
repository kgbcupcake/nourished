package dev.maire.nourished.nutrition.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.maire.nourished.Nourished;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
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
 * Writes scan reports in both human-readable (.txt) and machine-readable (.json) formats.
 */
public final class ScanReportWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ScanReportWriter() {}

    /**
     * Write all scan reports.
     *
     * @param results All classification results
     * @param summary The scan summary
     * @param diff Diff from previous scan (nullable)
     */
    public static void writeReports(
            List<ClassificationResult> results,
            ScanCache.ScanSummary summary,
            @Nullable ScanCache.ScanDiff diff
    ) throws IOException {
        Path outputDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Files.createDirectories(outputDir);

        writeTextReport(results, summary, diff, outputDir.resolve("unassigned_foods_report.txt"));
        writeJsonReport(results, summary, diff, outputDir.resolve("unassigned_foods_report.json"));

        if (diff != null && diff.hasChanges()) {
            writeDiffReport(diff, results, outputDir.resolve("unassigned_foods_diff.txt"));
        }
    }

    private static void writeTextReport(
            List<ClassificationResult> results,
            ScanCache.ScanSummary summary,
            @Nullable ScanCache.ScanDiff diff,
            Path outputFile
    ) throws IOException {
        Map<String, List<ClassificationResult>> byNamespace = groupByNamespace(results);
        List<ClassificationResult> uncertain = results.stream().filter(ClassificationResult::uncertain).toList();
        List<ClassificationResult> confident = results.stream().filter(r -> !r.uncertain()).toList();

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                        NOURISHED FOOD CLASSIFICATION REPORT\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");

            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("Mod List Hash: " + summary.modListHash() + "\n\n");

            writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
            writer.write("                                   SUMMARY\n");
            writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
            writer.write(String.format("  Total Scanned:     %d\n", summary.totalScanned()));
            writer.write(String.format("  Auto-Classified:   %d (confident)\n", summary.autoClassified()));
            writer.write(String.format("  Uncertain:         %d (needs review)\n", summary.uncertain()));
            writer.write(String.format("  Already Tagged:    %d (skipped)\n", summary.alreadyTagged()));
            writer.write("\n");

            if (diff != null && diff.hasChanges()) {
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("                              CHANGES FROM LAST SCAN\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write(String.format("  Added:   %d items\n", diff.added().size()));
                writer.write(String.format("  Changed: %d items\n", diff.changed().size()));
                writer.write(String.format("  Removed: %d items\n", diff.removed().size()));
                writer.write("\n");
            }

            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                           CONFIDENT CLASSIFICATIONS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");

            for (Map.Entry<String, List<ClassificationResult>> nsEntry : byNamespace.entrySet()) {
                List<ClassificationResult> nsConfident = nsEntry.getValue().stream()
                        .filter(r -> !r.uncertain())
                        .toList();

                if (nsConfident.isEmpty()) continue;

                writer.write("┌─ " + nsEntry.getKey() + " (" + nsConfident.size() + " items) ─────────────────\n");

                for (ClassificationResult r : nsConfident) {
                    String itemPath = r.itemId().getPath();
                    writer.write(String.format("│  %-40s → %-12s (spread: %.1f)\n",
                            truncate(itemPath, 40), r.dominant(), r.confidenceSpread()));

                    List<ClassificationSignal> topSignals = r.topSignals(3);
                    if (!topSignals.isEmpty()) {
                        StringBuilder signalStr = new StringBuilder();
                        for (ClassificationSignal sig : topSignals) {
                            if (signalStr.length() > 0) signalStr.append(", ");
                            signalStr.append(sig.signalType());
                        }
                        writer.write(String.format("│    └─ signals: %s\n", signalStr));
                    }
                }
                writer.write("└────────────────────────────────────────────────────────────────────────────\n\n");
            }

            if (!uncertain.isEmpty()) {
                writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
                writer.write("                          UNCERTAIN (NEEDS REVIEW)\n");
                writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
                writer.write("These items have low confidence spread and need manual classification.\n");
                writer.write("Add them to data/nourished/tags/item/nutrients/<category>.json\n\n");

                Map<String, List<ClassificationResult>> uncertainByNs = groupByNamespace(uncertain);
                for (Map.Entry<String, List<ClassificationResult>> nsEntry : uncertainByNs.entrySet()) {
                    writer.write("┌─ " + nsEntry.getKey() + " (" + nsEntry.getValue().size() + " items) ─────────────────\n");

                    for (ClassificationResult r : nsEntry.getValue()) {
                        writer.write(String.format("│  %-40s → %-12s vs %-12s (spread: %.1f)\n",
                                truncate(r.itemId().getPath(), 40),
                                r.dominant(),
                                r.secondary() != null ? r.secondary() : "none",
                                r.confidenceSpread()));
                    }
                    writer.write("└────────────────────────────────────────────────────────────────────────────\n\n");
                }
            }
        }
    }

    private static void writeJsonReport(
            List<ClassificationResult> results,
            ScanCache.ScanSummary summary,
            @Nullable ScanCache.ScanDiff diff,
            Path outputFile
    ) throws IOException {
        JsonObject root = new JsonObject();

        root.addProperty("scan_timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        root.addProperty("mod_list_hash", summary.modListHash());

        JsonObject summaryObj = new JsonObject();
        summaryObj.addProperty("total_scanned", summary.totalScanned());
        summaryObj.addProperty("auto_classified", summary.autoClassified());
        summaryObj.addProperty("uncertain", summary.uncertain());
        summaryObj.addProperty("already_tagged", summary.alreadyTagged());
        root.add("summary", summaryObj);

        JsonArray resultsArray = new JsonArray();
        JsonArray uncertainArray = new JsonArray();

        for (ClassificationResult r : results) {
            JsonObject item = resultToJson(r);
            if (r.uncertain()) {
                uncertainArray.add(item);
            } else {
                resultsArray.add(item);
            }
        }

        root.add("results", resultsArray);
        root.add("uncertain", uncertainArray);

        if (diff != null) {
            JsonObject diffObj = new JsonObject();

            JsonArray added = new JsonArray();
            for (ResourceLocation id : diff.added()) added.add(id.toString());
            diffObj.add("added", added);

            JsonArray changed = new JsonArray();
            for (ResourceLocation id : diff.changed()) changed.add(id.toString());
            diffObj.add("changed", changed);

            JsonArray removed = new JsonArray();
            for (ResourceLocation id : diff.removed()) removed.add(id.toString());
            diffObj.add("removed", removed);

            root.add("diff_from_previous", diffObj);
        }

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            GSON.toJson(root, writer);
        }
    }

    private static void writeDiffReport(
            ScanCache.ScanDiff diff,
            List<ClassificationResult> results,
            Path outputFile
    ) throws IOException {
        Map<ResourceLocation, ClassificationResult> resultMap = new LinkedHashMap<>();
        for (ClassificationResult r : results) {
            resultMap.put(r.itemId(), r);
        }

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                      NOURISHED SCAN DIFF REPORT\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n\n");

            if (!diff.added().isEmpty()) {
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("                              ADDED (" + diff.added().size() + ")\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                for (ResourceLocation id : diff.added()) {
                    ClassificationResult r = resultMap.get(id);
                    if (r != null) {
                        writer.write(String.format("  + %-50s → %s\n", id.toString(), r.dominant()));
                    } else {
                        writer.write(String.format("  + %s\n", id.toString()));
                    }
                }
                writer.write("\n");
            }

            if (!diff.changed().isEmpty()) {
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("                             CHANGED (" + diff.changed().size() + ")\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                for (ResourceLocation id : diff.changed()) {
                    ClassificationResult r = resultMap.get(id);
                    if (r != null) {
                        writer.write(String.format("  ~ %-50s → %s\n", id.toString(), r.dominant()));
                    } else {
                        writer.write(String.format("  ~ %s\n", id.toString()));
                    }
                }
                writer.write("\n");
            }

            if (!diff.removed().isEmpty()) {
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("                             REMOVED (" + diff.removed().size() + ")\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                for (ResourceLocation id : diff.removed()) {
                    writer.write(String.format("  - %s\n", id.toString()));
                }
                writer.write("\n");
            }
        }
    }

    private static JsonObject resultToJson(ClassificationResult r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item_id", r.itemId().toString());
        obj.addProperty("dominant", r.dominant());
        if (r.secondary() != null) {
            obj.addProperty("secondary", r.secondary());
        }
        obj.addProperty("confidence_spread", r.confidenceSpread());
        obj.addProperty("uncertain", r.uncertain());

        JsonObject scores = new JsonObject();
        for (Map.Entry<String, Float> e : r.scores().entrySet()) {
            scores.addProperty(e.getKey(), e.getValue());
        }
        obj.add("scores", scores);

        JsonArray signals = new JsonArray();
        for (ClassificationSignal sig : r.signals()) {
            JsonObject sigObj = new JsonObject();
            sigObj.addProperty("type", sig.signalType());
            sigObj.addProperty("source", sig.source());

            JsonObject contribs = new JsonObject();
            for (Map.Entry<String, Float> e : sig.contributions().entrySet()) {
                contribs.addProperty(e.getKey(), e.getValue());
            }
            sigObj.add("contributions", contribs);

            signals.add(sigObj);
        }
        obj.add("signals", signals);

        return obj;
    }

    private static Map<String, List<ClassificationResult>> groupByNamespace(List<ClassificationResult> results) {
        Map<String, List<ClassificationResult>> grouped = new LinkedHashMap<>();
        for (ClassificationResult r : results) {
            String ns = r.itemId().getNamespace();
            grouped.computeIfAbsent(ns, k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}
