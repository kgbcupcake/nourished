package dev.maire.nourished.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.NutrientDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Objects;

/**
 * Loads nutrient definitions from config/nourished/nutrients.json.
 * Writes a default file on first run. Call {@link #load()} before any
 * system that reads nutrient keys, icons, or tags.
 */
public class NutrientRegistry {

    public record NutrientDef(
            String key,
            String displayName,
            int color,
            float defaultDecayRate,
            float criticalThreshold,
            float lowThreshold,
            float excessThreshold,
            String icon,
            List<String> tags
    ) {
        public static NutrientDef fromDefinition(NutrientDefinition definition) {
            String key = Objects.requireNonNull(definition.getId(), "definition id");
            String icon = resolveIcon(key);
            List<String> tags = List.of("nourished:nutrients/" + key);
            return new NutrientDef(
                    key,
                    definition.getDisplayName(),
                    definition.getColor(),
                    definition.getDefaultDecayRate(),
                    definition.getCriticalThreshold(),
                    definition.getLowThreshold(),
                    definition.getExcessThreshold(),
                    icon,
                    tags
            );
        }

        public static NutrientDef fromConfig(String key, String icon, List<String> tags) {
            return new NutrientDef(
                    key,
                    key,
                    DEFAULT_COLOR,
                    DEFAULT_DECAY_RATE,
                    DEFAULT_CRITICAL_THRESHOLD,
                    DEFAULT_LOW_THRESHOLD,
                    DEFAULT_EXCESS_THRESHOLD,
                    icon,
                    tags
            );
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_ICON = "minecraft:apple";
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final float DEFAULT_DECAY_RATE = 0.001f;
    private static final float DEFAULT_CRITICAL_THRESHOLD = 0.1f;
    private static final float DEFAULT_LOW_THRESHOLD = 0.3f;
    private static final float DEFAULT_EXCESS_THRESHOLD = 0.9f;

    private static final Map<String, NutrientDef> REGISTRY = new LinkedHashMap<>();

    // ── Default definitions ───────────────────────────────────────────────────

    private static final List<NutrientDef> DEFAULTS = List.of(
        NutrientDef.fromConfig("fruits", "minecraft:golden_apple", List.of("nourished:nutrients/fruits")),
        NutrientDef.fromConfig("vegetables", "minecraft:carrot", List.of("nourished:nutrients/vegetables")),
        NutrientDef.fromConfig("proteins", "minecraft:cooked_beef", List.of("nourished:nutrients/proteins")),
        NutrientDef.fromConfig("grains", "minecraft:bread", List.of("nourished:nutrients/grains")),
        NutrientDef.fromConfig("sugars", "minecraft:sugar", List.of("nourished:nutrients/sugars")),
        NutrientDef.fromConfig("dairy", "minecraft:milk_bucket", List.of("nourished:nutrients/dairy"))
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

    public static void registerExternal(NutrientDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String key = Objects.requireNonNull(definition.getId(), "definition id");
        if (REGISTRY.containsKey(key)) {
            throw new IllegalArgumentException("Nutrient already registered: " + key);
        }
        REGISTRY.put(key, NutrientDef.fromDefinition(definition));
        Nourished.LOGGER.info("[NutrientRegistry] Registered external nutrient: {}", key);
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
                String icon = obj.has("icon") ? obj.get("icon").getAsString() : DEFAULT_ICON;
                List<String> tags = new ArrayList<>();
                if (obj.has("tags")) {
                    for (JsonElement t : obj.getAsJsonArray("tags")) {
                        tags.add(t.getAsString());
                    }
                }
                REGISTRY.put(key, NutrientDef.fromConfig(key, icon, Collections.unmodifiableList(tags)));
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

    private static String resolveIcon(String key) {
        String normalizedKey = Objects.requireNonNull(key, "key");
        ResourceLocation candidate = ResourceLocation.tryParse(normalizedKey);
        if (candidate != null && BuiltInRegistries.ITEM.containsKey(candidate)) {
            return candidate.toString();
        }
        ResourceLocation minecraftCandidate = ResourceLocation.tryParse("minecraft:" + normalizedKey);
        if (minecraftCandidate != null && BuiltInRegistries.ITEM.containsKey(minecraftCandidate)) {
            return minecraftCandidate.toString();
        }
        return DEFAULT_ICON;
    }
}
