package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NutrientDefinition;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.registry.AbstractRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads nutrient definitions from config/nourished/nutrients.json.
 * Writes a default file on first run. Call {@link #load()} before any
 * system that reads nutrient keys, icons, or tags.
 */
@ApiStatus.Internal
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
    private static final Map<String, String> ICON_FALLBACKS = Map.of(
            "fruits", "minecraft:apple",
            "vegetables", "minecraft:carrot",
            "proteins", "minecraft:cooked_beef",
            "grains", "minecraft:wheat",
            "sugars", "minecraft:sugar",
            "dairy", "minecraft:milk_bucket"
    );
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final float DEFAULT_DECAY_RATE = 0f;
    private static final float DEFAULT_CRITICAL_THRESHOLD = 0f;
    private static final float DEFAULT_LOW_THRESHOLD = 0f;
    private static final float DEFAULT_EXCESS_THRESHOLD = 1f;

    private static final class Core extends AbstractRegistry<String, NutrientDef> {
        Core() {
            super("NutrientRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private static final String[] DEFAULT_NUTRIENT_RESOURCES = {
            "fruits",
            "vegetables",
            "proteins",
            "grains",
            "sugars",
            "dairy"
    };

    // ── Public API ────────────────────────────────────────────────────────────

    /** Ordered list of nutrient keys (insertion order = display order). */
    public static List<String> getKeys() {
        return INSTANCE.keys();
    }

    /** Item ID string for the nutrient's icon, e.g. {@code "minecraft:carrot"}. */
    public static String getIcon(String key) {
        NutrientDef def = INSTANCE.get(key);
        if (def != null && def.icon() != null && !def.icon().isBlank()) {
            return def.icon();
        }
        return fallbackIconForKey(key);
    }

    /** Food tags that map to this nutrient. */
    public static List<String> getTags(String key) {
        NutrientDef def = INSTANCE.get(key);
        return def != null ? def.tags() : List.of();
    }

    /** All registered nutrient definitions in order. */
    public static List<NutrientDef> getAll() {
        return INSTANCE.values();
    }

    public static void registerExternal(NutrientDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String key = Objects.requireNonNull(definition.getId(), "definition id");
        if (INSTANCE.contains(key)) {
            throw new IllegalArgumentException("Nutrient already registered: " + key);
        }
        if (!INSTANCE.isFrozen()) {
            INSTANCE.register(key, NutrientDef.fromDefinition(definition));
            Nourished.LOGGER.info("[NutrientRegistry] Registered external nutrient: {}", key);
            return;
        }
        List<NutrientDef> prior = new ArrayList<>(INSTANCE.values());
        INSTANCE.reset();
        for (NutrientDef d : prior) {
            INSTANCE.register(d.key(), d);
        }
        INSTANCE.register(key, NutrientDef.fromDefinition(definition));
        INSTANCE.freeze();
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
            Nourished.LOGGER.info("[NutrientRegistry] Loaded {} nutrients from {}", INSTANCE.size(), file);
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
        INSTANCE.reset();
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            if (arr == null) {
                Nourished.LOGGER.warn("[NutrientRegistry] nutrients.json was empty, using built-in defaults");
                loadDefaults();
                return;
            }
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String key  = obj.get("key").getAsString();
                String icon = obj.has("icon") ? obj.get("icon").getAsString() : fallbackIconForKey(key);
                String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : key;
                int color = obj.has("color") ? obj.get("color").getAsInt() : DEFAULT_COLOR;
                float defaultDecayRate = obj.has("default_decay_rate") ? obj.get("default_decay_rate").getAsFloat() : DEFAULT_DECAY_RATE;
                float criticalThreshold = obj.has("critical_threshold") ? obj.get("critical_threshold").getAsFloat() : DEFAULT_CRITICAL_THRESHOLD;
                float lowThreshold = obj.has("low_threshold") ? obj.get("low_threshold").getAsFloat() : DEFAULT_LOW_THRESHOLD;
                float excessThreshold = obj.has("excess_threshold") ? obj.get("excess_threshold").getAsFloat() : DEFAULT_EXCESS_THRESHOLD;
                List<String> tags = new ArrayList<>();
                if (obj.has("tags")) {
                    for (JsonElement t : obj.getAsJsonArray("tags")) {
                        tags.add(t.getAsString());
                    }
                }
                INSTANCE.register(key, new NutrientDef(
                        key,
                        displayName,
                        color,
                        defaultDecayRate,
                        criticalThreshold,
                        lowThreshold,
                        excessThreshold,
                        icon,
                        Collections.unmodifiableList(tags)
                ));
            }
        }
        if (INSTANCE.size() == 0) {
            Nourished.LOGGER.warn("[NutrientRegistry] nutrients.json was empty, using built-in defaults");
            loadDefaults();
        } else {
            INSTANCE.freeze();
        }
    }

    private static void loadDefaults() {
        INSTANCE.reset();
        for (NutrientDef def : loadBundledDefaults()) {
            INSTANCE.register(def.key(), def);
        }
        INSTANCE.freeze();
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (NutrientDef def : loadBundledDefaults()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", def.key());
            obj.addProperty("display_name", def.displayName());
            obj.addProperty("color", def.color());
            obj.addProperty("default_decay_rate", def.defaultDecayRate());
            obj.addProperty("critical_threshold", def.criticalThreshold());
            obj.addProperty("low_threshold", def.lowThreshold());
            obj.addProperty("excess_threshold", def.excessThreshold());
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
        return fallbackIconForKey(normalizedKey);
    }

    private static String fallbackIconForKey(String key) {
        return ICON_FALLBACKS.getOrDefault(key, DEFAULT_ICON);
    }

    private static List<NutrientDef> loadBundledDefaults() {
        List<NutrientDef> defaults = new ArrayList<>();
        for (String path : DEFAULT_NUTRIENT_RESOURCES) {
            String resource = "/data/nourished/nourished/nutrients/" + path + ".json";
            try (InputStream in = NutrientRegistry.class.getResourceAsStream(resource)) {
                if (in == null) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                    if (obj == null) {
                        continue;
                    }
                    String key = path;
                    String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : key;
                    int color = obj.has("color") ? obj.get("color").getAsInt() : DEFAULT_COLOR;
                    float defaultDecayRate = obj.has("default_decay_rate") ? obj.get("default_decay_rate").getAsFloat() : DEFAULT_DECAY_RATE;
                    float criticalThreshold = obj.has("critical_threshold") ? obj.get("critical_threshold").getAsFloat() : DEFAULT_CRITICAL_THRESHOLD;
                    float lowThreshold = obj.has("low_threshold") ? obj.get("low_threshold").getAsFloat() : DEFAULT_LOW_THRESHOLD;
                    float excessThreshold = obj.has("excess_threshold") ? obj.get("excess_threshold").getAsFloat() : DEFAULT_EXCESS_THRESHOLD;
                    String icon = obj.has("icon") ? obj.get("icon").getAsString() : resolveIcon(key);
                    List<String> tags = List.of("nourished:nutrients/" + key);
                    defaults.add(new NutrientDef(key, displayName, color, defaultDecayRate, criticalThreshold, lowThreshold, excessThreshold, icon, tags));
                }
            } catch (IOException ignored) {
                // Keep fallback loading resilient.
            }
        }
        return defaults;
    }
}
