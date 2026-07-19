package dev.maire.nourished.core.nutrition.curve;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.curve.math.CurveGrid;
import dev.marie.framework.curve.serialization.CurveGridJson;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieJsonUtils;
import dev.marie.framework.util.MarieResourceLoader;
import dev.marie.framework.util.MarieValidation;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-nutrient response curve registry. Each nutrient resolves to a
 * {@link NutrientCurveDef} (preset-sourced or custom-authored grid).
 * Nutrients with no explicit entry fall back to the configured global
 * default preset (see NourishedConfig.defaultCurvePreset), and ultimately
 * to {@link NutrientCurvePreset#FLAT} if even that is unset.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>Hardcoded Java preset defaults (NutrientCurvePreset.FLAT for every nutrient)</li>
 *   <li>config/nourished/nutrient_curves.json (Cloth Config writes here; hand-editable)</li>
 *   <li>data/nourished/config/nutrient_curves.json (datapack override)</li>
 *   <li>KubeJS runtime registration (highest — see NutrientCurveRegistry.registerExternal,
 *       applied after datapack load in the same timing slot as custom nutrient registration)</li>
 * </ol>
 * Externally-registered (KubeJS) entries are tracked separately in
 * {@code EXTERNALLY_REGISTERED} so they survive reload() cycles that
 * repopulate from disk, matching the pattern established for
 * API-registered nutrient classifications.
 *
 * JSON schema (array of objects):
 * [
 *   { "nutrient": "protein", "preset": "DIMINISHING" },
 *   { "nutrient": "fiber", "preset": "custom", "grid": { "xCells": 5, "yCells": 5, "multipliers": [...] } }
 * ]
 */
@ApiStatus.Internal
public final class NutrientCurveRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, NutrientCurveDef> {
        Core() {
            super("NutrientCurveRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    /**
     * Nutrient keys registered externally via KubeJS/MarieAPI. These survive
     * reload() cycles that would otherwise wipe them when re-parsing from disk —
     * same pattern as EXTERNALLY_REGISTERED in the nutrient registration pipeline.
     */
    private static final Map<String, NutrientCurveDef> EXTERNALLY_REGISTERED = new ConcurrentHashMap<>();

    private NutrientCurveRegistry() {}

    /**
     * Resolves the curve for a nutrient: explicit registry entry if present,
     * else the configured global default preset, else FLAT.
     */
    public static CurveGrid resolve(String nutrientKey, String globalDefaultPresetId) {
        NutrientCurveDef def = INSTANCE.get(nutrientKey);
        if (def != null) {
            return def.grid();
        }
        NutrientCurvePreset fallback = parsePresetOrFlat(globalDefaultPresetId);
        return fallback.bake();
    }

    /** Returns the definition for a nutrient, or null if not explicitly set. */
    public static NutrientCurveDef get(String nutrientKey) {
        return INSTANCE.get(nutrientKey);
    }

    /** Returns all explicitly registered curve definitions. */
    public static List<NutrientCurveDef> getAll() {
        return INSTANCE.values();
    }

    /**
     * Registers a curve externally (KubeJS/MarieAPI). Tracked so it survives
     * reload() cycles. Must be called during the same registration window as
     * other external nutrient registrations.
     */
    public static void registerExternal(String nutrientKey, NutrientCurveDef def) {
        Objects.requireNonNull(nutrientKey, "nutrientKey");
        Objects.requireNonNull(def, "def");
        if (INSTANCE.contains(nutrientKey)) {
            throw new IllegalArgumentException("Nutrient curve already registered: " + nutrientKey);
        }
        if (!INSTANCE.isFrozen()) {
            INSTANCE.register(nutrientKey, def);
            EXTERNALLY_REGISTERED.put(nutrientKey, def);
            Nourished.LOGGER.info("[NutrientCurveRegistry] Registered external curve: {}", nutrientKey);
            return;
        }
        INSTANCE.runWrite(() -> {
            List<NutrientCurveDef> prior = new ArrayList<>(INSTANCE.valuesUnlocked());
            INSTANCE.resetUnlocked();
            for (NutrientCurveDef d : prior) {
                INSTANCE.registerUnlocked(d.nutrientKey(), d);
            }
            INSTANCE.registerUnlocked(nutrientKey, def);
            INSTANCE.freezeUnlocked();
        });
        EXTERNALLY_REGISTERED.put(nutrientKey, def);
        Nourished.LOGGER.info("[NutrientCurveRegistry] Registered external curve: {}", nutrientKey);
    }

    /**
     * Sets (or clears) a nutrient's explicit curve preset from the config screen.
     * Unlike {@link #registerExternal}, this freely overwrites any existing entry —
     * config screen edits are expected to change a prior selection, not fail on
     * duplicate. Not tracked in EXTERNALLY_REGISTERED since config screen entries
     * are persisted via the normal save()/load() JSON round-trip, not KubeJS reapply.
     *
     * @param nutrientKey the nutrient to set
     * @param presetId    a NutrientCurvePreset name, or null/blank to clear the
     *                    explicit entry and fall back to the global default
     */
    public static void setFromConfigScreen(String nutrientKey, @Nullable String presetId) {
        if (presetId == null || presetId.isBlank()) {
            INSTANCE.runWrite(() -> {
                List<NutrientCurveDef> prior = new ArrayList<>(INSTANCE.valuesUnlocked());
                INSTANCE.resetUnlocked();
                for (NutrientCurveDef d : prior) {
                    if (!d.nutrientKey().equals(nutrientKey)) {
                        INSTANCE.registerUnlocked(d.nutrientKey(), d);
                    }
                }
                reapplyExternallyRegisteredUnlocked();
                INSTANCE.freezeUnlocked();
            });
            return;
        }
        NutrientCurvePreset preset = parsePresetOrFlat(presetId);
        NutrientCurveDef def = NutrientCurveDef.fromPreset(nutrientKey, preset);
        INSTANCE.runWrite(() -> {
            List<NutrientCurveDef> prior = new ArrayList<>(INSTANCE.valuesUnlocked());
            INSTANCE.resetUnlocked();
            for (NutrientCurveDef d : prior) {
                if (!d.nutrientKey().equals(nutrientKey)) {
                    INSTANCE.registerUnlocked(d.nutrientKey(), d);
                }
            }
            INSTANCE.registerUnlocked(nutrientKey, def);
            reapplyExternallyRegisteredUnlocked();
            INSTANCE.freezeUnlocked();
        });
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("nutrient_curves.json");
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[NutrientCurveRegistry] Wrote default nutrient_curves.json");
            }
            parse(file);
            Nourished.LOGGER.info("[NutrientCurveRegistry] Loaded {} curve overrides from config folder", INSTANCE.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientCurveRegistry] Failed to load nutrient_curves.json, using preset defaults", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
        reapplyExternals();

        try {
            Path readmeDir = configDir.resolve("Read_Me");
            Files.createDirectories(readmeDir);
            writeReadmeIfAbsent(readmeDir);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NutrientCurveRegistry] Failed to write NUTRIENT_CURVES_README.md", e);
        }
    }

    private static void writeReadmeIfAbsent(Path readmeDir) throws IOException {
        Path readme = readmeDir.resolve("NUTRIENT_CURVES_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + Nourished.MODID + "/config/NUTRIENT_CURVES_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                Nourished.LOGGER.warn("[NutrientCurveRegistry] No bundled NUTRIENT_CURVES_README.md, skipping write");
                return;
            }
            Files.copy(in, readme);
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[NutrientCurveRegistry] Reloading nutrient_curves.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                "config/nutrient_curves.json",
                NutrientCurveRegistry::parseFromReader,
                NutrientCurveRegistry::load,
                "[NutrientCurveRegistry] Loaded from datapack override",
                "[NutrientCurveRegistry] Failed to load from datapack, falling back to config folder",
                "[NutrientCurveRegistry] Loaded from config folder"
        );
        reapplyExternals();
    }

    /**
     * Saves the current registry state back to nutrient_curves.json.
     * Used by the config screen to persist preset selections.
     */
    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("nutrient_curves.json");
        try {
            Files.createDirectories(configDir);
            writeCurrent(file);
            Nourished.LOGGER.info("[NutrientCurveRegistry] Saved nutrient_curves.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientCurveRegistry] Failed to save nutrient_curves.json", e);
        }
    }

    private static NutrientCurvePreset parsePresetOrFlat(String id) {
        if (id == null) {
            return NutrientCurvePreset.FLAT;
        }
        try {
            return NutrientCurvePreset.valueOf(id);
        } catch (IllegalArgumentException e) {
            return NutrientCurvePreset.FLAT;
        }
    }

    private static void reapplyExternals() {
        if (EXTERNALLY_REGISTERED.isEmpty()) {
            return;
        }
        boolean anyMissing = false;
        for (Map.Entry<String, NutrientCurveDef> entry : EXTERNALLY_REGISTERED.entrySet()) {
            NutrientCurveDef current = INSTANCE.get(entry.getKey());
            if (current == null || !current.equals(entry.getValue())) {
                anyMissing = true;
                break;
            }
        }
        if (!anyMissing) {
            return;
        }
        INSTANCE.runWrite(() -> {
            List<NutrientCurveDef> prior = new ArrayList<>(INSTANCE.valuesUnlocked());
            INSTANCE.resetUnlocked();
            for (NutrientCurveDef def : prior) {
                INSTANCE.registerUnlocked(def.nutrientKey(), def);
            }
            reapplyExternallyRegisteredUnlocked();
            INSTANCE.freezeUnlocked();
        });
    }

    private static void reapplyExternallyRegisteredUnlocked() {
        for (Map.Entry<String, NutrientCurveDef> entry : EXTERNALLY_REGISTERED.entrySet()) {
            if (!INSTANCE.keysUnlocked().contains(entry.getKey())) {
                INSTANCE.registerUnlocked(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean registerCurveRow(JsonObject obj, int index) {
        JsonElement nutrientEl = obj.get("nutrient");
        if (nutrientEl == null || !nutrientEl.isJsonPrimitive() || !nutrientEl.getAsJsonPrimitive().isString()) {
            Nourished.LOGGER.warn("[NutrientCurveRegistry] Skipping entry at index {} missing or invalid 'nutrient'", index);
            return false;
        }
        String nutrient = nutrientEl.getAsString();
        String presetStr = MarieJsonUtils.getOptionalString(obj, "preset", NutrientCurvePreset.FLAT.name());
        NutrientCurveDef def;
        if (NutrientCurveDef.CUSTOM_PRESET_ID.equalsIgnoreCase(presetStr)) {
            JsonElement gridEl = obj.get("grid");
            if (gridEl == null || !gridEl.isJsonObject()) {
                Nourished.LOGGER.warn("[NutrientCurveRegistry] Skipping entry at index {} missing or invalid 'grid' for custom preset", index);
                return false;
            }
            CurveGrid grid = CurveGridJson.fromJson(gridEl.getAsJsonObject());
            if (grid == null) {
                Nourished.LOGGER.warn("[NutrientCurveRegistry] Skipping entry at index {} malformed custom grid", index);
                return false;
            }
            def = NutrientCurveDef.custom(nutrient, grid);
        } else {
            def = NutrientCurveDef.fromPreset(nutrient, parsePresetOrFlat(presetStr));
        }
        INSTANCE.registerUnlocked(nutrient, def);
        return true;
    }

    private static void parse(Path file) throws IOException {
        JsonArray arr;
        try (Reader reader = Files.newBufferedReader(file)) {
            arr = GSON.fromJson(reader, JsonArray.class);
        }
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (arr == null) {
                Nourished.LOGGER.warn("[NutrientCurveRegistry] nutrient_curves.json was empty, using preset defaults");
                reapplyExternallyRegisteredUnlocked();
                INSTANCE.freezeUnlocked();
                return;
            }
            int limit = 2000;
            if (arr.size() > limit) {
                Nourished.LOGGER.warn("[NutrientCurveRegistry] nutrient_curves.json has {} entries, exceeding {} — truncating", arr.size(), limit);
            }
            for (int i = 0; i < Math.min(arr.size(), limit); i++) {
                registerCurveRow(arr.get(i).getAsJsonObject(), i);
            }
            reapplyExternallyRegisteredUnlocked();
            INSTANCE.freezeUnlocked();
        });
    }

    private static void parseFromReader(Reader reader) {
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (arr == null || arr.isEmpty()) {
                Nourished.LOGGER.warn("[NutrientCurveRegistry] Data was empty, using preset defaults");
                reapplyExternallyRegisteredUnlocked();
                INSTANCE.freezeUnlocked();
                return;
            }
            for (int i = 0; i < arr.size(); i++) {
                registerCurveRow(arr.get(i).getAsJsonObject(), i);
            }
            reapplyExternallyRegisteredUnlocked();
            INSTANCE.freezeUnlocked();
        });
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (String key : NutrientRegistry.getKeys()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("nutrient", key);
            obj.addProperty("preset", NutrientCurvePreset.FLAT.name());
            arr.add(obj);
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "NutrientCurveRegistry");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, writer);
        }
    }

    private static void writeCurrent(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (NutrientCurveDef def : INSTANCE.values()) {
            arr.add(defToJson(def));
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "NutrientCurveRegistry");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, writer);
        }
    }

    private static JsonObject defToJson(NutrientCurveDef def) {
        JsonObject obj = new JsonObject();
        obj.addProperty("nutrient", def.nutrientKey());
        if (NutrientCurveDef.CUSTOM_PRESET_ID.equals(def.presetId())) {
            obj.addProperty("preset", NutrientCurveDef.CUSTOM_PRESET_ID);
            obj.add("grid", CurveGridJson.toJson(def.grid()));
        } else {
            obj.addProperty("preset", def.presetId());
        }
        return obj;
    }
}
