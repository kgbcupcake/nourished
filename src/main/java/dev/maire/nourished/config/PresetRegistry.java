package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads and writes gameplay presets under {@code config/nourished/presets/}.
 * Built-in files are copied from the jar on first run when missing, similar to {@link dev.maire.nourished.core.effect.EffectRegistry}.
 */
public final class PresetRegistry {

    public static final Set<String> BUILTIN_STEMS = Set.of("casual", "survival", "hardcore");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record PresetValues(
            double decayRate,
            double criticalThreshold,
            double lowThreshold,
            double excessThreshold,
            int defaultEffectDurationTicks,
            boolean enableDecay,
            boolean enableEffects
    ) {
        public static PresetValues fromJsonObject(JsonObject values) {
            double decay = values.has("decayRate") ? values.get("decayRate").getAsDouble() : 0.1d;
            double crit = values.has("criticalThreshold") ? values.get("criticalThreshold").getAsDouble() : 0.25d;
            double low = values.has("lowThreshold") ? values.get("lowThreshold").getAsDouble() : 0.4d;
            double excess = values.has("excessThreshold") ? values.get("excessThreshold").getAsDouble() : 0.9d;
            int dur = values.has("defaultEffectDurationTicks") ? values.get("defaultEffectDurationTicks").getAsInt() : 140;
            boolean decayOn = !values.has("enableDecay") || values.get("enableDecay").getAsBoolean();
            boolean effectsOn = !values.has("enableEffects") || values.get("enableEffects").getAsBoolean();
            return new PresetValues(decay, crit, low, excess, dur, decayOn, effectsOn);
        }

        public static PresetValues fromCurrentConfig() {
            NourishedConfig c = NourishedConfig.get();
            return new PresetValues(
                    c.decayRate(),
                    c.criticalThreshold(),
                    c.lowThreshold(),
                    c.excessThreshold(),
                    c.defaultEffectDurationTicks(),
                    c.enableDecay(),
                    c.enableEffects()
            );
        }

        public JsonObject toJsonObject() {
            JsonObject o = new JsonObject();
            o.addProperty("decayRate", decayRate);
            o.addProperty("criticalThreshold", criticalThreshold);
            o.addProperty("lowThreshold", lowThreshold);
            o.addProperty("excessThreshold", excessThreshold);
            o.addProperty("defaultEffectDurationTicks", defaultEffectDurationTicks);
            o.addProperty("enableDecay", enableDecay);
            o.addProperty("enableEffects", enableEffects);
            return o;
        }
    }

    public record ParsedPreset(
            String fileStem,
            Path path,
            String name,
            String description,
            String author,
            boolean locked,
            boolean builtin,
            PresetValues values
    ) {
        public boolean canDelete() {
            return !builtin && !locked;
        }

        public boolean showLockIcon() {
            return locked || builtin;
        }
    }

    private PresetRegistry() {}

    public static Path presetsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("presets");
    }

    /**
     * Writes default Casual / Survival / Hardcore JSON files when they are absent.
     */
    public static void ensureBuiltInFilesOnDisk() {
        Path dir = presetsDirectory();
        try {
            Files.createDirectories(dir);
            for (String stem : BUILTIN_STEMS) {
                Path target = dir.resolve(stem + ".json");
                if (Files.exists(target)) {
                    continue;
                }
                String resource = "/data/nourished/nourished/presets/" + stem + ".json";
                try (InputStream in = Nourished.class.getResourceAsStream(resource)) {
                    if (in == null) {
                        Nourished.LOGGER.error("[PresetRegistry] Missing built-in resource {}", resource);
                        continue;
                    }
                    Files.copy(in, target);
                    Nourished.LOGGER.info("[PresetRegistry] Wrote built-in preset {}", target.getFileName());
                }
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[PresetRegistry] Failed to ensure built-in presets", e);
        }
    }

    public static void reload() {
        ensureBuiltInFilesOnDisk();
    }

    /**
     * Lists all {@code *.json} presets in the presets folder (newest files last after built-ins).
     */
    public static List<ParsedPreset> listPresets() {
        Path dir = presetsDirectory();
        List<ParsedPreset> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(p -> {
                        try {
                            ParsedPreset parsed = parseFile(p);
                            if (parsed != null) {
                                out.add(parsed);
                            }
                        } catch (Exception e) {
                            Nourished.LOGGER.warn("[PresetRegistry] Skipping invalid preset {}", p, e);
                        }
                    });
        } catch (IOException e) {
            Nourished.LOGGER.error("[PresetRegistry] Failed to list presets", e);
        }
        out.sort(Comparator
                .comparingInt((ParsedPreset p) -> builtinSortKey(p.fileStem()))
                .thenComparing(p -> p.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    private static int builtinSortKey(String stem) {
        String s = stem.toLowerCase(Locale.ROOT);
        if ("casual".equals(s)) {
            return 0;
        }
        if ("survival".equals(s)) {
            return 1;
        }
        if ("hardcore".equals(s)) {
            return 2;
        }
        return 10;
    }

    public static ParsedPreset parseFile(Path file) throws IOException {
        if (Files.size(file) > 65536) {
            throw new IOException("preset file too large: " + file);
        }
        String stem = fileNameStem(file);
        try (Reader r = Files.newBufferedReader(file)) {
            return parseReader(stem, file, r);
        }
    }

    private static ParsedPreset parseReader(String fileStem, Path path, Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            return null;
        }
        String name = root.has("name") ? root.get("name").getAsString() : fileStem;
        String description = root.has("description") ? root.get("description").getAsString() : "";
        String author = root.has("author") ? root.get("author").getAsString() : "";
        boolean locked = root.has("locked") && root.get("locked").getAsBoolean();
        boolean builtin = BUILTIN_STEMS.contains(fileStem.toLowerCase(Locale.ROOT));
        JsonElement valuesEl = root.get("values");
        if (valuesEl == null || !valuesEl.isJsonObject()) {
            Nourished.LOGGER.warn("[PresetRegistry] Preset {} has no values object", path);
            return null;
        }
        PresetValues values = PresetValues.fromJsonObject(valuesEl.getAsJsonObject());
        return new ParsedPreset(fileStem, path, name, description, author, locked, builtin, values);
    }

    private static String fileNameStem(Path file) {
        String fn = file.getFileName().toString();
        int dot = fn.lastIndexOf('.');
        return dot > 0 ? fn.substring(0, dot) : fn;
    }

    public static void applyPresetValues(PresetValues v) {
        NourishedConfig c = NourishedConfig.get();
        c.setDecayRate(clamp(v.decayRate(), 0.0d, 1.0d));
        c.setCriticalThreshold(clamp(v.criticalThreshold(), 0.0d, 1.0d));
        c.setLowThreshold(clamp(v.lowThreshold(), 0.0d, 1.0d));
        c.setExcessThreshold(clamp(v.excessThreshold(), 0.0d, 1.0d));
        c.setDefaultEffectDurationTicks(clamp(v.defaultEffectDurationTicks(), 20, 72000));
        c.setEnableDecay(v.enableDecay());
        c.setEnableEffects(v.enableEffects());
        NourishedConfig.saveNow();
    }

    public static void applyPreset(ParsedPreset preset) {
        applyPresetValues(preset.values());
        if ("hardcore".equalsIgnoreCase(preset.fileStem())) {
            enableAllEffects();
        }
    }

    private static void enableAllEffects() {
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
            Nourished.LOGGER.warn("[PresetRegistry] Failed to force-enable all effects for Hardcore preset", e);
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(max, Math.max(min, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(max, Math.max(min, v));
    }

    public static void deletePreset(ParsedPreset preset) throws IOException {
        if (!preset.canDelete()) {
            throw new IOException("Cannot delete built-in or locked preset");
        }
        Files.deleteIfExists(preset.path());
    }

    /**
     * Writes a user preset. {@code displayName} becomes the JSON {@code name}; the file stem is derived and made unique.
     */
    public static Path saveUserPreset(String displayName, String description, String author, PresetValues values) throws IOException {
        Path dir = presetsDirectory();
        Files.createDirectories(dir);
        String baseStem = sanitizeFileStem(displayName);
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
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(root, w);
        }
        Nourished.LOGGER.info("[PresetRegistry] Saved preset to {}", file);
        return file;
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

    /**
     * Lowercase file stem: letters, digits, underscore only.
     */
    public static String sanitizeFileStem(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lower.length() && sb.length() < 60; i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_') {
                sb.append(ch);
            } else if (ch == ' ' || ch == '-' || ch == '.') {
                sb.append('_');
            }
        }
        String s = sb.toString().replaceAll("_+", "_");
        if (s.startsWith("_")) {
            s = s.substring(1);
        }
        if (s.endsWith("_")) {
            s = s.substring(0, s.length() - 1);
        }
        if (BUILTIN_STEMS.contains(s)) {
            return s + "_custom";
        }
        return s;
    }

}
