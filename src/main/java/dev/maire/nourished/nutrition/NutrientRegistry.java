package dev.maire.nourished.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.Nourished;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads nutrient definitions from config/nourished/nutrients.json.
 * Writes a default file on first run. Call {@link #load()} before any
 * system that reads nutrient keys, icons, or tags.
 */
public class NutrientRegistry {

    public record NutrientDef(String key, String icon, List<String> tags) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, NutrientDef> REGISTRY = new LinkedHashMap<>();

    // ── Default definitions ───────────────────────────────────────────────────

    private static final List<NutrientDef> DEFAULTS = List.of(
        new NutrientDef("fruits",     "minecraft:golden_apple", List.of("nourished:nutrients/fruits")),
        new NutrientDef("vegetables", "minecraft:carrot", List.of("nourished:nutrients/vegetables")),
        new NutrientDef("proteins",   "minecraft:cooked_beef", List.of("nourished:nutrients/proteins")),
        new NutrientDef("grains",     "minecraft:bread", List.of("nourished:nutrients/grains")),
        new NutrientDef("sugars",     "minecraft:sugar", List.of("nourished:nutrients/sugars")),
        new NutrientDef("dairy",      "minecraft:milk_bucket", List.of("nourished:nutrients/dairy"))
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /** Ordered list of nutrient keys (insertion order = display order). */
    public static List<String> getKeys() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.keySet()));
    }

    /** Item ID string for the nutrient's icon, e.g. {@code "minecraft:carrot"}. */
    public static String getIcon(String key) {
        NutrientDef def = REGISTRY.get(key);
        return def != null ? def.icon() : "minecraft:apple";
    }

    /** Food tags that map to this nutrient. */
    public static List<String> getTags(String key) {
        NutrientDef def = REGISTRY.get(key);
        return def != null ? def.tags() : List.of();
    }

    /** All registered nutrient definitions in order. */
    public static List<NutrientDef> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.values()));
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("nutrients.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[NutrientRegistry] Wrote default nutrients.json");
            }
            parse(file);
            Nourished.LOGGER.info("[NutrientRegistry] Loaded {} nutrients from {}", REGISTRY.size(), file);
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientRegistry] Failed to load nutrients.json, using built-in defaults", e);
            loadDefaults();
        }
    }

    /**
     * Re-reads nutrients.json from disk and re-runs {@link FoodNutritionRegistry#init()}.
     * Safe to call at runtime; dependent systems are updated immediately after.
     */
    public static void reload() {
        Nourished.LOGGER.info("[NutrientRegistry] Reloading nutrients.json");
        load();
        FoodNutritionRegistry.init();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static void parse(Path file) throws IOException {
        REGISTRY.clear();
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String key  = obj.get("key").getAsString();
                String icon = obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:apple";
                List<String> tags = new ArrayList<>();
                if (obj.has("tags")) {
                    for (JsonElement t : obj.getAsJsonArray("tags")) {
                        tags.add(t.getAsString());
                    }
                }
                REGISTRY.put(key, new NutrientDef(key, icon, Collections.unmodifiableList(tags)));
            }
        }
        if (REGISTRY.isEmpty()) {
            Nourished.LOGGER.warn("[NutrientRegistry] nutrients.json was empty, using built-in defaults");
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        REGISTRY.clear();
        for (NutrientDef def : DEFAULTS) {
            REGISTRY.put(def.key(), def);
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (NutrientDef def : DEFAULTS) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", def.key());
            obj.addProperty("icon", def.icon());
            JsonArray tags = new JsonArray();
            for (String t : def.tags()) tags.add(t);
            obj.add("tags", tags);
            arr.add(obj);
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
