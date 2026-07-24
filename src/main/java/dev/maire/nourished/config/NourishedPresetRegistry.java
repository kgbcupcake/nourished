package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.framework.config.PresetRegistry;
import dev.marie.framework.data.DatapackSchema;
import dev.marie.framework.util.MarieValidation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Nourished-specific preset storage under {@code config/nourished/presets/}.
 */
public final class NourishedPresetRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> BUILTIN_STEMS = List.of("casual", "survival", "hardcore");

    private NourishedPresetRegistry() {}

    public static Path presetsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("presets");
    }

    public static void ensureBuiltInFilesOnDisk() {
        Path dir = presetsDirectory();
        try {
            Files.createDirectories(dir);
            for (String stem : BUILTIN_STEMS) {
                Path target = dir.resolve(stem + ".json");
                if (Files.exists(target)) {
                    continue;
                }
                String resource = "/data/" + Nourished.MODID + "/" + DatapackSchema.root() + "/presets/" + stem + ".json";
                try (InputStream in = Nourished.class.getResourceAsStream(resource)) {
                    if (in == null) {
                        Nourished.LOGGER.error("[NourishedPresetRegistry] Missing built-in resource {}", resource);
                        continue;
                    }
                    Files.copy(in, target);
                    Nourished.LOGGER.info("[NourishedPresetRegistry] Wrote built-in preset {}", target.getFileName());
                }
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[NourishedPresetRegistry] Failed to ensure built-in presets", e);
        }
    }

    public static void reload() {
        ensureBuiltInFilesOnDisk();
    }

    public static List<PresetRegistry.ParsedPreset> listPresets() {
        Path dir = presetsDirectory();
        List<PresetRegistry.ParsedPreset> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(p -> {
                        try {
                            PresetRegistry.ParsedPreset parsed = PresetRegistry.parseFile(p);
                            if (parsed != null) {
                                out.add(parsed);
                            }
                        } catch (Exception e) {
                            Nourished.LOGGER.warn("[NourishedPresetRegistry] Skipping invalid preset {}", p, e);
                        }
                    });
        } catch (IOException e) {
            Nourished.LOGGER.error("[NourishedPresetRegistry] Failed to list presets", e);
        }
        out.sort(Comparator
                .comparing((PresetRegistry.ParsedPreset p) -> p.locked())
                .reversed()
                .thenComparing(p -> p.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    public static void applyPreset(PresetRegistry.ParsedPreset preset) {
        applyPresetValues(preset.values());
        if ("hardcore".equals(preset.fileStem())) {
            enableAllEffects();
        }
    }

    public static void applyPresetValues(PresetRegistry.PresetValues preset) {
        JsonObject v = preset.toJsonObject();
        NourishedConfig config = NourishedConfig.get();
        if (v.has("decayRate"))                  config.setDecayRate(clamp(v.get("decayRate").getAsDouble(), 0.0d, 1.0d));
        if (v.has("criticalThreshold"))          config.setCriticalThreshold(clamp(v.get("criticalThreshold").getAsDouble(), 0.0d, 1.0d));
        if (v.has("lowThreshold"))               config.setLowThreshold(clamp(v.get("lowThreshold").getAsDouble(), 0.0d, 1.0d));
        if (v.has("excessThreshold"))            config.setExcessThreshold(clamp(v.get("excessThreshold").getAsDouble(), 0.0d, 1.0d));
        if (v.has("defaultEffectDurationTicks")) config.setDefaultEffectDurationTicks(clamp(v.get("defaultEffectDurationTicks").getAsInt(), 20, 72000));
        if (v.has("enableDecay"))                config.setEnableDecay(v.get("enableDecay").getAsBoolean());
        if (v.has("enableEffects"))              config.setEnableEffects(v.get("enableEffects").getAsBoolean());
        NourishedConfig.saveNow();
        NourishedConfig.syncModuleCache();
    }

    public static void deletePreset(PresetRegistry.ParsedPreset preset) throws IOException {
        PresetRegistry.deletePreset(preset);
    }

    public static Path saveUserPreset(
            String displayName,
            String description,
            String author,
            PresetRegistry.PresetValues values
    ) throws IOException {
        Path dir = presetsDirectory();
        Files.createDirectories(dir);
        String baseStem = PresetRegistry.sanitizeFileStem(displayName);
        if (baseStem.isEmpty()) {
            baseStem = "preset";
        }
        String stem = uniquifyStem(dir, baseStem);
        Path file = dir.resolve(stem + ".json");
        JsonObject root = new JsonObject();
        root.addProperty("name", displayName.trim());
        root.addProperty("description", description == null ? "" : description.trim());
        root.addProperty("author", author == null ? "" : author.trim());
        root.addProperty("locked", false);
        root.add("values", values.toJsonObject());
        MarieValidation.assertPathUnder(file, dir, "NourishedPresetRegistry.saveUserPreset");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(root, w);
        }
        Nourished.LOGGER.info("[NourishedPresetRegistry] Saved preset to {}", file);
        return file;
    }

    public static void enableAllEffects() {
        List<EffectRegistry.EffectDef> current = EffectRegistry.getAll();
        List<EffectRegistry.EffectDef> updated = new ArrayList<>(current.size());
        for (EffectRegistry.EffectDef def : current) {
            updated.add(new EffectRegistry.EffectDef(
                    def.id(),
                    def.effect(),
                    def.nutrient(),
                    def.trigger(),
                    def.threshold(),
                    def.amplifier(),
                    def.durationTicks(),
                    true,
                    def.thresholdMax(),
                    def.ambient(),
                    def.showParticles()
            ));
        }
        try {
            EffectRegistry.saveAll(updated);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedPresetRegistry] Failed to force-enable all effects for Hardcore preset", e);
        }
    }

    private static String uniquifyStem(Path dir, String base) throws IOException {
        String candidate = base;
        int n = 2;
        while (Files.exists(dir.resolve(candidate + ".json"))) {
            candidate = base + "_" + n;
            n++;
        }
        return candidate;
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
