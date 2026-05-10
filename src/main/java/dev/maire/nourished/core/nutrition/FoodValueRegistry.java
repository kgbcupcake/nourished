package dev.maire.nourished.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

/**
 * Loads per-category food value multipliers from config/nourished/food_values.json.
 * Each category maps to five multipliers (protein, carbs, fats, vitamins, hydration).
 * Writes a default file on first run. Call {@link #load()} after {@link NutrientRegistry#load()}.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>Hardcoded Java defaults (fallback only)</li>
 *   <li>config/nourished/food_values.json</li>
 *   <li>data/nourished/config/food_values.json (datapack override)</li>
 * </ol>
 */
@ApiStatus.Internal
public class FoodValueRegistry {

    public record FoodValueDef(String category, float protein, float carbs, float fats, float vitamins, float hydration) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, FoodValueDef> REGISTRY = new LinkedHashMap<>();

    private static final float[] DEFAULT_WEIGHTS = {0.2f, 0.2f, 0.2f, 0.2f, 0.2f};

    private static final String DEFAULT_RESOURCE_PATH = "/data/nourished/config/food_values.json";

    /**
     * Returns NutrientValues scaled by the category's multipliers.
     * Falls back to an even split if the category is unknown.
     */
    public static FoodNutritionRegistry.NutrientValues getValuesForCategory(String categoryKey, float totalPoints) {
        FoodValueDef def = REGISTRY.get(categoryKey);
        if (def == null) {
            return new FoodNutritionRegistry.NutrientValues(
                    totalPoints * DEFAULT_WEIGHTS[0],
                    totalPoints * DEFAULT_WEIGHTS[1],
                    totalPoints * DEFAULT_WEIGHTS[2],
                    totalPoints * DEFAULT_WEIGHTS[3],
                    totalPoints * DEFAULT_WEIGHTS[4]
            );
        }
        return new FoodNutritionRegistry.NutrientValues(
                totalPoints * def.protein(),
                totalPoints * def.carbs(),
                totalPoints * def.fats(),
                totalPoints * def.vitamins(),
                totalPoints * def.hydration()
        );
    }

    /** Returns the definition for a category, or null if not found. */
    public static FoodValueDef get(String categoryKey) {
        return REGISTRY.get(categoryKey);
    }

    /** Returns all registered food value definitions. */
    public static List<FoodValueDef> getAll() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(REGISTRY.values()));
    }

    /** Returns all category keys. */
    public static List<String> getCategories() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(REGISTRY.keySet()));
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("food_values.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[FoodValueRegistry] Wrote default food_values.json");
            }
            parse(file);
            Nourished.LOGGER.info("[FoodValueRegistry] Loaded {} categories from config folder", REGISTRY.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[FoodValueRegistry] Failed to load food_values.json, using built-in defaults", e);
            loadDefaults();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[FoodValueRegistry] Reloading food_values.json");
        load();
    }

    /**
     * Attempts to load from datapack first, then falls back to config folder.
     * Call this from a reload listener when datapacks are available.
     */
    public static void loadFromDatapack(ResourceManager resourceManager) {
        ResourceLocation datapackPath = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "config/food_values.json");
        Optional<Resource> resource = resourceManager.getResource(datapackPath);

        if (resource.isPresent()) {
            try (InputStream is = resource.get().open();
                 Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                parseFromReader(r);
                Nourished.LOGGER.info("[FoodValueRegistry] Loaded from datapack override");
                return;
            } catch (IOException e) {
                Nourished.LOGGER.error("[FoodValueRegistry] Failed to load from datapack, falling back to config folder", e);
            }
        }

        load();
        Nourished.LOGGER.info("[FoodValueRegistry] Loaded from config folder");
    }

    private static void parseFromReader(Reader reader) {
        REGISTRY.clear();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr == null || arr.isEmpty()) {
            Nourished.LOGGER.warn("[FoodValueRegistry] Data was empty, using built-in defaults");
            loadDefaults();
            return;
        }
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String category = obj.get("category").getAsString();
            float protein   = obj.has("protein")   ? obj.get("protein").getAsFloat()   : 0.2f;
            float carbs     = obj.has("carbs")     ? obj.get("carbs").getAsFloat()     : 0.2f;
            float fats      = obj.has("fats")      ? obj.get("fats").getAsFloat()      : 0.2f;
            float vitamins  = obj.has("vitamins")  ? obj.get("vitamins").getAsFloat()  : 0.2f;
            float hydration = obj.has("hydration") ? obj.get("hydration").getAsFloat() : 0.2f;
            REGISTRY.put(category, new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
        }
        if (REGISTRY.isEmpty()) {
            loadDefaults();
        }
    }

    /**
     * Saves the current registry state back to food_values.json.
     * Used by the config screen to persist changes.
     */
    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("food_values.json");
        try {
            writeRegistry(file);
            Nourished.LOGGER.info("[FoodValueRegistry] Saved food_values.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[FoodValueRegistry] Failed to save food_values.json", e);
        }
    }

    /**
     * Updates or adds a category definition in the registry.
     */
    public static void setCategory(String category, float protein, float carbs, float fats, float vitamins, float hydration) {
        REGISTRY.put(category, new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
    }

    private static void parse(Path file) throws IOException {
        REGISTRY.clear();
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            if (arr == null) {
                Nourished.LOGGER.warn("[FoodValueRegistry] food_values.json was empty, using built-in defaults");
                loadDefaults();
                return;
            }
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String category = obj.get("category").getAsString();
                float protein   = obj.has("protein")   ? obj.get("protein").getAsFloat()   : 0.2f;
                float carbs     = obj.has("carbs")     ? obj.get("carbs").getAsFloat()     : 0.2f;
                float fats      = obj.has("fats")      ? obj.get("fats").getAsFloat()      : 0.2f;
                float vitamins  = obj.has("vitamins")  ? obj.get("vitamins").getAsFloat()  : 0.2f;
                float hydration = obj.has("hydration") ? obj.get("hydration").getAsFloat() : 0.2f;
                REGISTRY.put(category, new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
            }
        }
        if (REGISTRY.isEmpty()) {
            Nourished.LOGGER.warn("[FoodValueRegistry] food_values.json was empty, using built-in defaults");
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        REGISTRY.clear();
        for (FoodValueDef def : loadBundledDefaults()) {
            REGISTRY.put(def.category(), def);
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (FoodValueDef def : loadBundledDefaults()) {
            arr.add(defToJson(def));
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (FoodValueDef def : REGISTRY.values()) {
            arr.add(defToJson(def));
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static JsonObject defToJson(FoodValueDef def) {
        JsonObject obj = new JsonObject();
        obj.addProperty("category", def.category());
        obj.addProperty("protein", def.protein());
        obj.addProperty("carbs", def.carbs());
        obj.addProperty("fats", def.fats());
        obj.addProperty("vitamins", def.vitamins());
        obj.addProperty("hydration", def.hydration());
        return obj;
    }

    private static List<FoodValueDef> loadBundledDefaults() {
        List<FoodValueDef> defaults = new ArrayList<>();
        try (InputStream in = FoodValueRegistry.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in == null) {
                return defaults;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonArray arr = GSON.fromJson(reader, JsonArray.class);
                if (arr == null) {
                    return defaults;
                }
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String category = obj.get("category").getAsString();
                    float protein = obj.has("protein") ? obj.get("protein").getAsFloat() : DEFAULT_WEIGHTS[0];
                    float carbs = obj.has("carbs") ? obj.get("carbs").getAsFloat() : DEFAULT_WEIGHTS[1];
                    float fats = obj.has("fats") ? obj.get("fats").getAsFloat() : DEFAULT_WEIGHTS[2];
                    float vitamins = obj.has("vitamins") ? obj.get("vitamins").getAsFloat() : DEFAULT_WEIGHTS[3];
                    float hydration = obj.has("hydration") ? obj.get("hydration").getAsFloat() : DEFAULT_WEIGHTS[4];
                    defaults.add(new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
                }
            }
        } catch (IOException ignored) {
            // Keep load resilient if bundled defaults are unavailable.
        }
        return defaults;
    }
}
