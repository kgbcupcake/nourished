package dev.maire.nourished.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.client.MarieValueColors;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import dev.marie.MariesLib.color.ColorRegistry;
import dev.marie.MariesLib.config.FeatureFlagCache;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.util.MarieJsonUtils;
import dev.marie.MariesLib.util.MarieValidation;
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
 * Nourished-specific config import/export serialization wired into {@link dev.marie.MariesLib.core.MarieLibContext}.
 */
public final class NourishedImportExport {

    public static final int SCHEMA_VERSION = 1;
    private static final String SHARE_PREFIX = "NCF1:";
    private static final Gson GSON_COMPACT = new GsonBuilder().create();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public enum Section {
        GENERAL("general"),
        THRESHOLDS("thresholds"),
        EFFECTS("effects"),
        VALUE_COLORS("valueColors"),
        SOURCE_VALUES("sourceValues"),
        MODULES("modules");

        private final String jsonKey;

        Section(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String jsonKey() {
            return jsonKey;
        }

        public String labelKey() {
            return switch (this) {
                case VALUE_COLORS -> "nutrient_colors";
                case SOURCE_VALUES -> "food_values";
                default -> name().toLowerCase(Locale.ROOT);
            };
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

    private NourishedImportExport() {}

    public static String sharePrefix() {
        return SHARE_PREFIX;
    }

    public static Path exportsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("exports");
    }

    public static Path writeExportFile(JsonObject root) throws IOException {
        Files.createDirectories(exportsDirectory());
        String stem = Nourished.MODID + "-config-" + LocalDateTime.now().format(FILE_TS);
        Path file = exportsDirectory().resolve(stem + ".json");
        MarieValidation.assertPathUnder(file, exportsDirectory(), "NourishedImportExport.writeExportFile");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON_PRETTY.toJson(root, w);
        }
        return file;
    }

    public static JsonObject buildExportRoot(Set<Section> sections) {
        JsonObject full = exportCurrentConfig();
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        for (Section s : sections) {
            if (full.has(s.jsonKey()) && !full.get(s.jsonKey()).isJsonNull()) {
                root.add(s.jsonKey(), full.get(s.jsonKey()));
            }
        }
        return root;
    }

    public static String buildShareCode(JsonObject root) throws IOException {
        String json = GSON_COMPACT.toJson(root);
        byte[] gzipped = gzipUtf8(json);
        return sharePrefix() + Base64.getEncoder().encodeToString(gzipped);
    }

    public static JsonObject parseShareCode(String raw) throws IOException {
        if (raw == null) {
            throw new IOException("empty");
        }
        String prefix = sharePrefix();
        String flat = raw.replaceAll("\\s+", "");
        if (!flat.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new IOException("missing prefix");
        }
        String b64 = flat.substring(prefix.length());
        byte[] decoded = Base64.getDecoder().decode(b64);
        if (decoded.length > 65536) {
            throw new IOException("share code payload too large: " + decoded.length + " bytes");
        }
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

    public static JsonObject exportCurrentConfig() {
        NourishedConfig c = NourishedConfig.get();
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.add(Section.MODULES.jsonKey(), exportModules(c));
        root.add(Section.GENERAL.jsonKey(), exportGeneral(c));
        root.add(Section.THRESHOLDS.jsonKey(), exportThresholds(c));
        root.add(Section.EFFECTS.jsonKey(), exportEffects(c));
        root.add(Section.VALUE_COLORS.jsonKey(), exportNutrientColors());
        root.add(Section.SOURCE_VALUES.jsonKey(), exportFoodValues());
        return root;
    }

    public static void applyImport(JsonObject root) throws IOException {
        applyImport(root, sectionsPresent(root));
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
        if (selected.contains(Section.VALUE_COLORS) && root.has(Section.VALUE_COLORS.jsonKey())) {
            applyNutrientColors(root.get(Section.VALUE_COLORS.jsonKey()));
        }
        if (selected.contains(Section.SOURCE_VALUES) && root.has(Section.SOURCE_VALUES.jsonKey())) {
            applyFoodValues(root.get(Section.SOURCE_VALUES.jsonKey()));
        }
        NourishedConfig.saveNow();
        NourishedReloadHelper.reloadAll();
        MarieValueColors.clearOverrides();
    }

    public static PresetRegistry.PresetValues presetValuesFromCurrentConfig() {
        NourishedConfig c = NourishedConfig.get();
        JsonObject o = new JsonObject();
        o.addProperty("decayRate", c.decayRate());
        o.addProperty("criticalThreshold", c.criticalThreshold());
        o.addProperty("lowThreshold", c.lowThreshold());
        o.addProperty("excessThreshold", c.excessThreshold());
        o.addProperty("defaultEffectDurationTicks", c.defaultEffectDurationTicks());
        o.addProperty("enableDecay", c.enableDecay());
        o.addProperty("enableEffects", c.enableEffects());
        return PresetRegistry.PresetValues.fromJsonObject(o);
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
        o.addProperty("enableSleepBonus", FeatureFlagCache.enableSleepBonus());
        o.addProperty("enableSynergies", FeatureFlagCache.enableSynergies());
        o.addProperty("enableMilestones", FeatureFlagCache.enableMilestones());
        o.addProperty("enableSeasonHooks", FeatureFlagCache.enableSeasonHooks());
        o.addProperty("enableAbsorptionModifiers", FeatureFlagCache.enableAbsorptionModifiers());
        return o;
    }

    private static JsonObject exportGeneral(NourishedConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("decayRate", c.decayRate());
        o.addProperty("decayIntervalTicks", c.decayIntervalTicks());
        o.addProperty("startingNutrientValue", c.startingNutrientValue());
        o.addProperty("deathNutritionBehavior", c.deathNutritionBehaviorConfigId());
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
        if (o.has("enableSleepBonus")) {
            c.setModuleEnabled("enableSleepBonus", o.get("enableSleepBonus").getAsBoolean());
        }
        if (o.has("enableSynergies")) {
            c.setModuleEnabled("enableSynergies", o.get("enableSynergies").getAsBoolean());
        }
        if (o.has("enableMilestones")) {
            c.setModuleEnabled("enableMilestones", o.get("enableMilestones").getAsBoolean());
        }
        if (o.has("enableSeasonHooks")) {
            c.setModuleEnabled("enableSeasonHooks", o.get("enableSeasonHooks").getAsBoolean());
        }
        if (o.has("enableAbsorptionModifiers")) {
            c.setModuleEnabled("enableAbsorptionModifiers", o.get("enableAbsorptionModifiers").getAsBoolean());
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
        if (o.has("deathNutritionBehavior")) {
            c.setDeathNutritionBehavior(o.get("deathNutritionBehavior").getAsString());
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
            MarieValidation.requireBoundedMap(map.asMap(), 64, "applyGeneral.nutrientDecayOverrides");
            for (String key : NutrientRegistry.getKeys()) {
                if (!map.has(key)) {
                    continue;
                }
                ModConfigSpec.DoubleValue spec = c.nutrientDecayRateOverrides().get(key);
                if (spec != null) {
                    double v = map.get(key).getAsDouble();
                    spec.set(Double.isFinite(v) ? clamp(v, 0d, 1d) : 0d);
                }
            }
        }
        if (o.has("nutrientCriticalOverrides") && o.get("nutrientCriticalOverrides").isJsonObject()) {
            JsonObject map = o.getAsJsonObject("nutrientCriticalOverrides");
            MarieValidation.requireBoundedMap(map.asMap(), 64, "applyGeneral.nutrientCriticalOverrides");
            for (String key : NutrientRegistry.getKeys()) {
                if (!map.has(key)) {
                    continue;
                }
                ModConfigSpec.DoubleValue spec = c.nutrientCriticalThresholdOverrides().get(key);
                if (spec != null) {
                    double v = map.get(key).getAsDouble();
                    spec.set(Double.isFinite(v) ? clamp(v, 0d, 1d) : 0d);
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
            JsonArray arr = o.getAsJsonArray("definitions");
            MarieValidation.requireBoundedArray(arr, 256, "applyEffects.definitions");
            List<EffectRegistry.EffectDef> defs = parseEffectDefs(arr);
            EffectRegistry.saveAll(defs);
        }
    }

    @com.google.common.annotations.VisibleForTesting
    static List<EffectRegistry.EffectDef> parseEffectDefs(JsonArray arr) throws IOException {
        List<EffectRegistry.EffectDef> out = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("id")) throw new IOException("missing required field: id in effect definition");
            if (!obj.has("effect")) throw new IOException("missing required field: effect in effect definition");
            if (!obj.has("nutrient")) throw new IOException("missing required field: nutrient in effect definition");
            String id = obj.get("id").getAsString();
            String effect = obj.get("effect").getAsString();
            String nutrient = obj.get("nutrient").getAsString();
            MarieValidation.requireBoundedString(id, 256, "parseEffectDefs.id");
            MarieValidation.requireBoundedString(effect, 256, "parseEffectDefs.effect");
            MarieValidation.requireBoundedString(nutrient, 256, "parseEffectDefs.nutrient");
            String trigger = obj.has("trigger") ? obj.get("trigger").getAsString() : "below";
            MarieValidation.requireBoundedString(trigger, 256, "parseEffectDefs.trigger");
            double threshold = MarieJsonUtils.getOptionalDouble(obj, "threshold", 0.25d);
            threshold = clamp(threshold, 0d, 1d);
            int amplifier = MarieJsonUtils.getOptionalInt(obj, "amplifier", 0);
            int durationTicks = MarieJsonUtils.getOptionalInt(obj, "duration_ticks", 140);
            boolean enabled = MarieJsonUtils.getOptionalBoolean(obj, "enabled", true);
            double thresholdMax = MarieJsonUtils.getOptionalDouble(obj, "threshold_max", 1.0d);
            thresholdMax = clamp(thresholdMax, 0d, 1d);
            boolean ambient = MarieJsonUtils.getOptionalBoolean(obj, "ambient", true);
            boolean showParticles = MarieJsonUtils.getOptionalBoolean(obj, "show_particles", false);
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
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(el.getAsJsonArray(), w);
        }
    }

    private static void applyFoodValues(JsonElement el) throws IOException {
        if (!el.isJsonArray()) {
            return;
        }
        JsonArray arr = el.getAsJsonArray();
        MarieValidation.requireBoundedArray(arr, 256, "applyFoodValues");
        if (arr.isEmpty()) {
            return;
        }
        for (JsonElement row : arr) {
            if (!row.isJsonObject()) {
                continue;
            }
            JsonObject obj = row.getAsJsonObject();
            String category = obj.get("category").getAsString();
            MarieValidation.requireBoundedString(category, 256, "applyFoodValues.category");
            float protein = clampF(MarieJsonUtils.getOptionalFloat(obj, "protein", 0.2f), 0f, 10f);
            float carbs = clampF(MarieJsonUtils.getOptionalFloat(obj, "carbs", 0.2f), 0f, 10f);
            float fats = clampF(MarieJsonUtils.getOptionalFloat(obj, "fats", 0.2f), 0f, 10f);
            float vitamins = clampF(MarieJsonUtils.getOptionalFloat(obj, "vitamins", 0.2f), 0f, 10f);
            float hydration = clampF(MarieJsonUtils.getOptionalFloat(obj, "hydration", 0.2f), 0f, 10f);
            FoodValueRegistry.setCategory(category, protein, carbs, fats, vitamins, hydration);
        }
        FoodValueRegistry.save();
    }

    private static float clampF(float v, float min, float max) {
        return Float.isFinite(v) ? Math.min(max, Math.max(min, v)) : min;
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

    @com.google.common.annotations.VisibleForTesting
    static String gunzipUtf8(byte[] data) throws IOException {
        final int maxBytes = 1_048_576;
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
    Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
StringWriter sw = new StringWriter();) {
            char[] buf = new char[8192];
            int total = 0;
            int n;
            while ((n = r.read(buf)) != -1) {
                total += n;
                if (total > maxBytes) {
                    throw new IOException("decompressed payload exceeds 1 MiB");
                }
                sw.write(buf, 0, n);
            }
            return sw.toString();
        }
    }
}
