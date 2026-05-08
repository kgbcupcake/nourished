package dev.maire.nourished.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads per-item food overrides from config/nourished/food_overrides.json.
 * This is an escape hatch for modpack creators who want to override specific items.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>Hardcoded Java defaults (empty)</li>
 *   <li>config/nourished/food_overrides.json</li>
 *   <li>data/nourished/config/food_overrides.json (datapack override)</li>
 * </ol>
 */
public class FoodOverrideRegistry {

    /**
     * An override for a specific food item.
     * @param item The item ID (e.g. "minecraft:golden_apple")
     * @param nutrients Map of nutrient key to delta value
     * @param calories Calorie value for this item
     * @param enabled Whether this override is active
     */
    public record FoodOverride(String item, Map<String, Float> nutrients, int calories, boolean enabled) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, FoodOverride> REGISTRY = new LinkedHashMap<>();

    /**
     * Returns the override for an item, if one exists and is enabled.
     */
    public static Optional<FoodOverride> getOverride(String itemId) {
        FoodOverride override = REGISTRY.get(itemId);
        if (override != null && override.enabled()) {
            return Optional.of(override);
        }
        return Optional.empty();
    }

    /**
     * Returns the override for an item (whether enabled or not), or null if none.
     */
    public static FoodOverride get(String itemId) {
        return REGISTRY.get(itemId);
    }

    /** Returns all registered overrides. */
    public static Map<String, FoodOverride> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("food_overrides.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[FoodOverrideRegistry] Wrote default food_overrides.json");
            }
            parse(file);
            Nourished.LOGGER.info("[FoodOverrideRegistry] Loaded {} overrides from config folder", REGISTRY.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[FoodOverrideRegistry] Failed to load food_overrides.json", e);
            REGISTRY.clear();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[FoodOverrideRegistry] Reloading food_overrides.json");
        load();
    }

    /**
     * Attempts to load from datapack first, then falls back to config folder.
     * Call this from a reload listener when datapacks are available.
     */
    public static void loadFromDatapack(ResourceManager resourceManager) {
        ResourceLocation datapackPath = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "config/food_overrides.json");
        Optional<Resource> resource = resourceManager.getResource(datapackPath);

        if (resource.isPresent()) {
            try (InputStream is = resource.get().open();
                 Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                parseFromReader(r);
                Nourished.LOGGER.info("[FoodOverrideRegistry] Loaded from datapack override");
                return;
            } catch (IOException e) {
                Nourished.LOGGER.error("[FoodOverrideRegistry] Failed to load from datapack, falling back to config folder", e);
            }
        }

        load();
        Nourished.LOGGER.info("[FoodOverrideRegistry] Loaded from config folder");
    }

    private static void parseFromReader(Reader reader) {
        REGISTRY.clear();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr == null) {
            return;
        }
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String item = obj.get("item").getAsString();
            int calories = obj.has("calories") ? obj.get("calories").getAsInt() : 0;
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();

            Map<String, Float> nutrients = new HashMap<>();
            if (obj.has("nutrients") && obj.get("nutrients").isJsonObject()) {
                JsonObject nutrientsObj = obj.getAsJsonObject("nutrients");
                for (Map.Entry<String, JsonElement> entry : nutrientsObj.entrySet()) {
                    nutrients.put(entry.getKey(), entry.getValue().getAsFloat());
                }
            }

            REGISTRY.put(item, new FoodOverride(item, nutrients, calories, enabled));
        }
    }

    /**
     * Saves the current registry state back to food_overrides.json.
     */
    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("food_overrides.json");
        try {
            writeRegistry(file);
            Nourished.LOGGER.info("[FoodOverrideRegistry] Saved food_overrides.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[FoodOverrideRegistry] Failed to save food_overrides.json", e);
        }
    }

    /**
     * Adds or updates an override.
     */
    public static void setOverride(String item, Map<String, Float> nutrients, int calories, boolean enabled) {
        REGISTRY.put(item, new FoodOverride(item, new HashMap<>(nutrients), calories, enabled));
    }

    /**
     * Removes an override.
     */
    public static void removeOverride(String item) {
        REGISTRY.remove(item);
    }

    private static void parse(Path file) throws IOException {
        REGISTRY.clear();
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            if (arr == null) {
                return;
            }
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String item = obj.get("item").getAsString();
                int calories = obj.has("calories") ? obj.get("calories").getAsInt() : 0;
                boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();

                Map<String, Float> nutrients = new HashMap<>();
                if (obj.has("nutrients") && obj.get("nutrients").isJsonObject()) {
                    JsonObject nutrientsObj = obj.getAsJsonObject("nutrients");
                    for (Map.Entry<String, JsonElement> entry : nutrientsObj.entrySet()) {
                        nutrients.put(entry.getKey(), entry.getValue().getAsFloat());
                    }
                }

                REGISTRY.put(item, new FoodOverride(item, nutrients, calories, enabled));
            }
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (FoodOverride override : REGISTRY.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("item", override.item());
            
            JsonObject nutrientsObj = new JsonObject();
            for (Map.Entry<String, Float> entry : override.nutrients().entrySet()) {
                nutrientsObj.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("nutrients", nutrientsObj);
            
            obj.addProperty("calories", override.calories());
            obj.addProperty("enabled", override.enabled());
            arr.add(obj);
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
