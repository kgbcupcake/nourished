package dev.maire.nourished.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.color.ColorRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.nutrition.FoodValueRegistry;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and restores bundled Nourished settings for file export and {@code NCF1:} share codes.
 */
public final class ImportExportManager {

    public static final String SHARE_PREFIX = "NCF1:";
    public static final int SCHEMA_VERSION = 1;

    private static final Gson GSON_COMPACT = new GsonBuilder().create();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public enum Section {
        GENERAL("general"),
        THRESHOLDS("thresholds"),
        EFFECTS("effects"),
        NUTRIENT_COLORS("nutrientColors"),
        FOOD_VALUES("foodValues"),
        MODULES("modules");

        private final String jsonKey;

        Section(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String jsonKey() {
            return jsonKey;
        }

        public static Section fromJsonKey(String key) {
            for (Section s : values()) {
                if (s.jsonKey.equals(key)) {
                    return s;
                }
            }
            return null;
        }
    }

    private ImportExportManager() {}

    public static Path exportsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("exports");
    }

    public static Path writeExportFile(JsonObject root) throws IOException {
        Files.createDirectories(exportsDirectory());
        String stem = "nourished-config-" + LocalDateTime.now().format(FILE_TS);
        Path file = exportsDirectory().resolve(stem + ".json");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON_PRETTY.toJson(root, w);
        }
        return file;
    }

    public static JsonObject buildExportRoot(Set<Section> sections) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        NourishedConfig c = NourishedConfig.get();
        if (sections.contains(Section.MODULES)) {
            root.add(Section.MODULES.jsonKey(), exportModules(c));
        }
        if (sections.contains(Section.GENERAL)) {
            root.add(Section.GENERAL.jsonKey(), exportGeneral(c));
        }
        if (sections.contains(Section.THRESHOLDS)) {
            root.add(Section.THRESHOLDS.jsonKey(), exportThresholds(c));
        }
        if (sections.contains(Section.EFFECTS)) {
            root.add(Section.EFFECTS.jsonKey(), exportEffects(c));
        }
        if (sections.contains(Section.NUTRIENT_COLORS)) {
            root.add(Section.NUTRIENT_COLORS.jsonKey(), exportNutrientColors());
        }
        if (sections.contains(Section.FOOD_VALUES)) {
            root.add(Section.FOOD_VALUES.jsonKey(), exportFoodValues());
        }
        return root;
    }

    public static String buildShareCode(JsonObject root) throws IOException {
        String json = GSON_COMPACT.toJson(root);
        byte[] gzipped = gzipUtf8(json);
        return SHARE_PREFIX + Base64.getEncoder().encodeToString(gzipped);
    }

    public static JsonObject parseShareCode(String raw) throws IOException {
        if (raw == null) {
            throw new IOException("empty");
        }
        String flat = raw.replaceAll("\\s+", "");
        if (!flat.regionMatches(true, 0, SHARE_PREFIX, 0, SHARE_PREFIX.length())) {
            throw new IOException("missing prefix");
        }
        String b64 = flat.substring(SHARE_PREFIX.length());
        byte[] decoded = Base64.getDecoder().decode(b64);
        String json = gunzipUtf8(decoded);
        JsonObject obj = GSON_COMPACT.fromJson(json, JsonObject.class);
        if (obj == null) {
            throw new IOException("not json object");
        }
        return obj;
    }

    public static JsonObject parseJsonFile(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = GSON_COMPACT.fromJson(r, JsonObject.class);
            if (obj == null) {
                throw new IOException("empty file");
            }
            return obj;
        }
    }

    public static EnumSet<Section> sectionsPresent(JsonObject root) {
        EnumSet<Section> out = EnumSet.noneOf(Section.class);
        for (Section s : Section.values()) {
            if (root.has(s.jsonKey()) && !root.get(s.jsonKey()).isJsonNull()) {
                out.add(s);
            }
        }
        return out;
    }

    public static void applyImport(JsonObject root, Set<Section> selected) throws IOException {
        NourishedConfig c = NourishedConfig.get();
        if (selected.contains(Section.MODULES) && root.has(Section.MODULES.jsonKey())) {
            applyModules(c, root.getAsJsonObject(Section.MODULES.jsonKey()));
        }
        if (selected.contains(Section.GENERAL) && root.has(Section.GENERAL.jsonKey())) {
            applyGeneral(c, root.getAsJsonObject(Section.GENERAL.jsonKey()));
        }
        if (selected.contains(Section.THRESHOLDS) && root.has(Section.THRESHOLDS.jsonKey())) {
            applyThresholds(c, root.getAsJsonObject(Section.THRESHOLDS.jsonKey()));
        }
        if (selected.contains(Section.EFFECTS) && root.has(Section.EFFECTS.jsonKey())) {
            applyEffects(c, root.getAsJsonObject(Section.EFFECTS.jsonKey()));
        }
        if (selected.contains(Section.NUTRIENT_COLORS) && root.has(Section.NUTRIENT_COLORS.jsonKey())) {
            applyNutrientColors(root.get(Section.NUTRIENT_COLORS.jsonKey()));
        }
        if (selected.contains(Section.FOOD_VALUES) && root.has(Section.FOOD_VALUES.jsonKey())) {
            applyFoodValues(root.get(Section.FOOD_VALUES.jsonKey()));
        }
        NourishedConfig.saveNow();
        EffectRegistry.reload();
        ColorRegistry.reload();
        FoodValueRegistry.reload();
        NutrientUiColors.clearOverrides();
    }

    public static List<Path> listExportJsonFiles() throws IOException {
        Path dir = exportsDirectory();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .forEach(out::add);
        }
        return out;
    }

    private static JsonObject exportModules(NourishedConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("enableDecay", c.enableDecay());
        o.addProperty("enableEffects", c.enableEffects());
        o.addProperty("enableHUD", c.enableHUD());
        o.addProperty("enableToasts", c.enableToasts());
        o.addProperty("enableFoodTooltips", c.enableFoodTooltips());
        o.addProperty("enableCalorieTracking", c.enableCalorieTracking());
        o.addProperty("enableDietScreen", c.enableDietScreen());
        o.addProperty("enableCriticalToasts", c.enableCriticalToasts());
        return o;
    }

    private static JsonObject exportGeneral(NourishedConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("decayRate", c.decayRate());
        o.addProperty("decayIntervalTicks", c.decayIntervalTicks());
        o.addProperty("startingNutrientValue", c.startingNutrientValue());
        o.addProperty("nutrientGainScale", c.nutrientGainScale());
        o.addProperty("nutrientGainPerBiteMax", c.nutrientGainPerBiteMax());
        o.addProperty("calorieDisplayMax", c.calorieDisplayMax());
        JsonObject decay = new JsonObject();
        JsonObject crit = new JsonObject();
        for (String key : NutrientRegistry.getKeys()) {
            ModConfigSpec.DoubleValue dv = c.nutrientDecayRateOverrides().get(key);
            if (dv != null) {
                decay.addProperty(key, dv.get());
            }
            ModConfigSpec.DoubleValue cv = c.nutrientCriticalThresholdOverrides().get(key);
            if (cv != null) {
                crit.addProperty(key, cv.get());
            }
        }
        o.add("nutrientDecayOverrides", decay);
        o.add("nutrientCriticalOverrides", crit);
        return o;
    }

    private static JsonObject exportThresholds(NourishedConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("criticalThreshold", c.criticalThreshold());
        o.addProperty("lowThreshold", c.lowThreshold());
        o.addProperty("excessThreshold", c.excessThreshold());
        o.addProperty("bonusEffectThreshold", c.bonusEffectThreshold());
        o.addProperty("penaltyEffectThreshold", c.penaltyEffectThreshold());
        return o;
    }

    private static JsonObject exportEffects(NourishedConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("defaultEffectDurationTicks", c.defaultEffectDurationTicks());
        JsonArray defs = new JsonArray();
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            JsonObject el = new JsonObject();
            el.addProperty("id", def.id());
            el.addProperty("effect", def.effect());
            el.addProperty("nutrient", def.nutrient());
            el.addProperty("trigger", def.trigger());
            el.addProperty("threshold", def.threshold());
            el.addProperty("amplifier", def.amplifier());
            el.addProperty("duration_ticks", def.durationTicks());
            el.addProperty("enabled", def.enabled());
            el.addProperty("threshold_max", def.thresholdMax());
            el.addProperty("ambient", def.ambient());
            el.addProperty("show_particles", def.showParticles());
            defs.add(el);
        }
        o.add("definitions", defs);
        return o;
    }

    private static JsonArray exportNutrientColors() {
        JsonArray arr = new JsonArray();
        for (String key : NutrientRegistry.getKeys()) {
            ColorRegistry.getArgb(key).ifPresent(argb -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("key", key);
                obj.addProperty("argb", String.format(Locale.ROOT, "0x%08X", argb));
                arr.add(obj);
            });
        }
        return arr;
    }

    private static JsonArray exportFoodValues() {
        JsonArray arr = new JsonArray();
        for (FoodValueRegistry.FoodValueDef def : FoodValueRegistry.getAll()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("category", def.category());
            obj.addProperty("protein", def.protein());
            obj.addProperty("carbs", def.carbs());
            obj.addProperty("fats", def.fats());
            obj.addProperty("vitamins", def.vitamins());
            obj.addProperty("hydration", def.hydration());
            arr.add(obj);
        }
        return arr;
    }

    private static void applyModules(NourishedConfig c, JsonObject o) {
        if (o.has("enableDecay")) {
            c.setEnableDecay(o.get("enableDecay").getAsBoolean());
        }
        if (o.has("enableEffects")) {
            c.setEnableEffects(o.get("enableEffects").getAsBoolean());
        }
        if (o.has("enableHUD")) {
            c.setEnableHUD(o.get("enableHUD").getAsBoolean());
        }
        if (o.has("enableToasts")) {
            c.setEnableToasts(o.get("enableToasts").getAsBoolean());
        }
        if (o.has("enableFoodTooltips")) {
            c.setEnableFoodTooltips(o.get("enableFoodTooltips").getAsBoolean());
        }
        if (o.has("enableCalorieTracking")) {
            c.setEnableCalorieTracking(o.get("enableCalorieTracking").getAsBoolean());
        }
        if (o.has("enableDietScreen")) {
            c.setEnableDietScreen(o.get("enableDietScreen").getAsBoolean());
        }
        if (o.has("enableCriticalToasts")) {
            c.setEnableCriticalToasts(o.get("enableCriticalToasts").getAsBoolean());
        }
    }

    private static void applyGeneral(NourishedConfig c, JsonObject o) {
        if (o.has("decayRate")) {
            c.setDecayRate(clamp(o.get("decayRate").getAsDouble(), 0d, 1d));
        }
        if (o.has("decayIntervalTicks")) {
            c.setDecayIntervalTicks(clampInt(o.get("decayIntervalTicks").getAsInt(), 1, Integer.MAX_VALUE));
        }
        if (o.has("startingNutrientValue")) {
            c.setStartingNutrientValue(clamp(o.get("startingNutrientValue").getAsDouble(), 0d, 1d));
        }
        if (o.has("nutrientGainScale")) {
            c.setNutrientGainScale(clamp(o.get("nutrientGainScale").getAsDouble(), 0.5d, 20d));
        }
        if (o.has("nutrientGainPerBiteMax")) {
            c.setNutrientGainPerBiteMax(clamp(o.get("nutrientGainPerBiteMax").getAsDouble(), 0.05d, 1d));
        }
        if (o.has("calorieDisplayMax")) {
            c.setCalorieDisplayMax(clampInt(o.get("calorieDisplayMax").getAsInt(), 100, 100_000));
        }
        if (o.has("nutrientDecayOverrides") && o.get("nutrientDecayOverrides").isJsonObject()) {
            JsonObject map = o.getAsJsonObject("nutrientDecayOverrides");
            for (String key : NutrientRegistry.getKeys()) {
                if (!map.has(key)) {
                    continue;
                }
                ModConfigSpec.DoubleValue spec = c.nutrientDecayRateOverrides().get(key);
                if (spec != null) {
                    spec.set(map.get(key).getAsDouble());
                }
            }
        }
        if (o.has("nutrientCriticalOverrides") && o.get("nutrientCriticalOverrides").isJsonObject()) {
            JsonObject map = o.getAsJsonObject("nutrientCriticalOverrides");
            for (String key : NutrientRegistry.getKeys()) {
                if (!map.has(key)) {
                    continue;
                }
                ModConfigSpec.DoubleValue spec = c.nutrientCriticalThresholdOverrides().get(key);
                if (spec != null) {
                    spec.set(map.get(key).getAsDouble());
                }
            }
        }
    }

    private static void applyThresholds(NourishedConfig c, JsonObject o) {
        if (o.has("criticalThreshold")) {
            c.setCriticalThreshold(clamp(o.get("criticalThreshold").getAsDouble(), 0d, 1d));
        }
        if (o.has("lowThreshold")) {
            c.setLowThreshold(clamp(o.get("lowThreshold").getAsDouble(), 0d, 1d));
        }
        if (o.has("excessThreshold")) {
            c.setExcessThreshold(clamp(o.get("excessThreshold").getAsDouble(), 0d, 1d));
        }
        if (o.has("bonusEffectThreshold")) {
            c.setBonusEffectThreshold(clamp(o.get("bonusEffectThreshold").getAsDouble(), 0d, 1d));
        }
        if (o.has("penaltyEffectThreshold")) {
            c.setPenaltyEffectThreshold(clamp(o.get("penaltyEffectThreshold").getAsDouble(), 0d, 1d));
        }
    }

    private static void applyEffects(NourishedConfig c, JsonObject o) throws IOException {
        if (o.has("defaultEffectDurationTicks")) {
            c.setDefaultEffectDurationTicks(clampInt(o.get("defaultEffectDurationTicks").getAsInt(), 20, 72000));
        }
        if (o.has("definitions") && o.get("definitions").isJsonArray()) {
            List<EffectRegistry.EffectDef> defs = parseEffectDefs(o.getAsJsonArray("definitions"));
            EffectRegistry.saveAll(defs);
        }
    }

    private static List<EffectRegistry.EffectDef> parseEffectDefs(JsonArray arr) {
        List<EffectRegistry.EffectDef> out = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String effect = obj.get("effect").getAsString();
            String nutrient = obj.get("nutrient").getAsString();
            String trigger = obj.get("trigger").getAsString();
            double threshold = obj.has("threshold") ? obj.get("threshold").getAsDouble() : 0.25d;
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            int durationTicks = obj.has("duration_ticks") ? obj.get("duration_ticks").getAsInt() : 140;
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            double thresholdMax = obj.has("threshold_max") ? obj.get("threshold_max").getAsDouble() : 1.0d;
            boolean ambient = !obj.has("ambient") || obj.get("ambient").getAsBoolean();
            boolean showParticles = obj.has("show_particles") && obj.get("show_particles").getAsBoolean();
            out.add(new EffectRegistry.EffectDef(
                    id,
                    effect,
                    nutrient,
                    trigger,
                    threshold,
                    amplifier,
                    durationTicks,
                    enabled,
                    thresholdMax,
                    ambient,
                    showParticles));
        }
        return out;
    }

    private static void applyNutrientColors(JsonElement el) throws IOException {
        if (!el.isJsonArray()) {
            return;
        }
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Files.createDirectories(configDir);
        Path file = configDir.resolve("colors.json");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON_PRETTY.toJson(el.getAsJsonArray(), w);
        }
    }

    private static void applyFoodValues(JsonElement el) throws IOException {
        if (!el.isJsonArray()) {
            return;
        }
        JsonArray arr = el.getAsJsonArray();
        if (arr.isEmpty()) {
            return;
        }
        for (JsonElement row : arr) {
            if (!row.isJsonObject()) {
                continue;
            }
            JsonObject obj = row.getAsJsonObject();
            String category = obj.get("category").getAsString();
            float protein = obj.has("protein") ? obj.get("protein").getAsFloat() : 0.2f;
            float carbs = obj.has("carbs") ? obj.get("carbs").getAsFloat() : 0.2f;
            float fats = obj.has("fats") ? obj.get("fats").getAsFloat() : 0.2f;
            float vitamins = obj.has("vitamins") ? obj.get("vitamins").getAsFloat() : 0.2f;
            float hydration = obj.has("hydration") ? obj.get("hydration").getAsFloat() : 0.2f;
            FoodValueRegistry.setCategory(category, protein, carbs, fats, vitamins, hydration);
        }
        FoodValueRegistry.save();
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(max, Math.max(min, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.min(max, Math.max(min, v));
    }

    private static byte[] gzipUtf8(String s) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(s.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private static String gunzipUtf8(byte[] data) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
             Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
             StringWriter sw = new StringWriter()) {
            r.transferTo(sw);
            return sw.toString();
        }
    }
}
