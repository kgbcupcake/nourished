package dev.maire.nourished.core.tagaudit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tagaudit.model.TagFixSuggestion;
import dev.marie.framework.tagaudit.model.TagIssue;
import dev.marie.framework.tagaudit.model.TagReport;
import dev.marie.framework.util.MarieValidation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes Nourished tag audit output to {@code config/nourished/tag_audit_report.json}.
 * Lives in Nourished (not MarieLib) so the command works against published MarieLib jars
 * that predate {@code TagAuditReportWriter}.
 */
@ApiStatus.Internal
public final class NourishedTagAuditReportWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private NourishedTagAuditReportWriter() {}

    public static Path write(TagReport report) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("tag_audit_report.json");
        try {
            Files.createDirectories(configDir);
            MarieValidation.assertPathUnder(file, configDir, "NourishedTagAuditReportWriter.write");

            JsonObject root = new JsonObject();
            root.addProperty("modId", Nourished.MODID);
            root.addProperty("timestamp", report.timestamp());
            root.addProperty("rulesRun", String.join(", ", report.rulesRun()));

            JsonArray issuesArr = new JsonArray();
            for (TagIssue issue : report.issues()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("issueId", issue.issueId());
                obj.addProperty("itemId", issue.itemId().toString());
                obj.addProperty("currentTag", issue.currentTag());
                obj.addProperty("ruleId", issue.ruleId());
                obj.addProperty("confidence", issue.confidence());
                obj.addProperty("severity", issue.severity().name());
                obj.addProperty("reason", issue.reason());
                issuesArr.add(obj);
            }
            root.add("issues", issuesArr);

            JsonArray suggestionsArr = new JsonArray();
            for (TagFixSuggestion suggestion : report.suggestions()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("issueId", suggestion.issueId());
                obj.addProperty("suggestedTag", suggestion.suggestedTag());
                obj.addProperty("ruleId", suggestion.ruleId());
                obj.addProperty("confidence", suggestion.confidence());
                obj.addProperty("reason", suggestion.reason());
                suggestionsArr.add(obj);
            }
            root.add("suggestions", suggestionsArr);

            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(root, w);
            }
            Nourished.LOGGER.info("[NourishedTagAuditReportWriter] Wrote tag audit report ({} issues, {} suggestions) to {}",
                    report.issues().size(), report.suggestions().size(), file);
            return file;
        } catch (IOException e) {
            Nourished.LOGGER.error("[NourishedTagAuditReportWriter] Failed to write tag audit report", e);
            return null;
        }
    }
}
