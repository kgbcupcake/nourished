package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.export.RegistryExporter;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NutrientFullExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Result(boolean success, int filesWritten, Path exportDir, @Nullable Exception error) {}

    private NutrientFullExporter() {}

    public static Result run() {
        Path exportDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("nourished_nutrients_export");

        Map<ResourceLocation, Map<String, Object>> results =
                RegistryExporter.run("nourished_nutrients");
        if (results.isEmpty()) {
            Nourished.LOGGER.error("[NutrientFullExporter] Full nutrient export produced no data");
            return new Result(false, 0, exportDir, null);
        }

        Map<String, List<Map<String, Object>>> byCategory = new LinkedHashMap<>();
        for (String k : NutrientRegistry.getKeys()) {
            byCategory.put(k, new ArrayList<>());
        }

        for (Map.Entry<ResourceLocation, Map<String, Object>> e : results.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nutrients = (Map<String, Object>) e.getValue().get("nutrients");
            if (nutrients == null || nutrients.isEmpty()) {
                continue;
            }
            String dominant = null;
            double best = Double.NEGATIVE_INFINITY;
            for (Map.Entry<String, Object> n : nutrients.entrySet()) {
                double v = ((Number) n.getValue()).doubleValue();
                if (v > best) {
                    best = v;
                    dominant = n.getKey();
                }
            }
            if (dominant == null || !byCategory.containsKey(dominant)) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("item", e.getKey().toString());
            entry.putAll(e.getValue());
            byCategory.get(dominant).add(entry);
        }

        try {
            MarieValidation.assertPathUnder(
                    exportDir.normalize(),
                    FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).normalize(),
                    "fullExport");
            Files.createDirectories(exportDir);

            int written = 0;
            for (Map.Entry<String, List<Map<String, Object>>> e : byCategory.entrySet()) {
                if (e.getValue().isEmpty()) {
                    continue;
                }
                Path file = exportDir.resolve(e.getKey() + ".json");
                try (Writer w = Files.newBufferedWriter(file)) {
                    GSON.toJson(e.getValue(), w);
                }
                written++;
            }

            if (written == 0) {
                Nourished.LOGGER.error("[NutrientFullExporter] Full nutrient export produced no categorized data");
                return new Result(false, 0, exportDir, null);
            }

            return new Result(true, written, exportDir, null);
        } catch (IOException ex) {
            Nourished.LOGGER.error("[NutrientFullExporter] Full export failed", ex);
            return new Result(false, 0, exportDir, ex);
        }
    }
}
