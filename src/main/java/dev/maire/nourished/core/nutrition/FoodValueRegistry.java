package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.marie.MariesLib.registry.AbstractRegistry;
import dev.marie.MariesLib.util.MarieJsonUtils;
import dev.marie.MariesLib.util.MarieResourceLoader;
import dev.marie.MariesLib.util.MarieValidation;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public record NutrientValues(float protein, float carbs, float fats, float vitamins, float hydration) {}

    public record FoodValueDef(String category, float protein, float carbs, float fats, float vitamins, float hydration) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, FoodValueDef> {
        Core() {
            super("FoodValueRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private static final float[] DEFAULT_WEIGHTS = {0.2f, 0.2f, 0.2f, 0.2f, 0.2f};

    private static final String DEFAULT_RESOURCE_PATH = "/data/" + Nourished.MODID + "/config/food_values.json";

    /**
     * Returns NutrientValues scaled by the category's multipliers.
     * Falls back to an even split if the category is unknown.
     */
    public static NutrientValues getValuesForCategory(String categoryKey, float totalPoints) {
        FoodValueDef def = INSTANCE.get(categoryKey);
        if (def == null) {
            return new NutrientValues(
                    totalPoints * DEFAULT_WEIGHTS[0],
                    totalPoints * DEFAULT_WEIGHTS[1],
                    totalPoints * DEFAULT_WEIGHTS[2],
                    totalPoints * DEFAULT_WEIGHTS[3],
                    totalPoints * DEFAULT_WEIGHTS[4]
            );
        }
        return new NutrientValues(
                totalPoints * def.protein(),
                totalPoints * def.carbs(),
                totalPoints * def.fats(),
                totalPoints * def.vitamins(),
                totalPoints * def.hydration()
        );
    }

    /** Returns the definition for a category, or null if not found. */
    public static FoodValueDef get(String categoryKey) {
        return INSTANCE.get(categoryKey);
    }

    /** Returns all registered food value definitions. */
    public static List<FoodValueDef> getAll() {
        return INSTANCE.values();
    }

    /** Returns all category keys. */
    public static List<String> getCategories() {
        return INSTANCE.keys();
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
            Nourished.LOGGER.info("[FoodValueRegistry] Loaded {} categories from config folder", INSTANCE.size());
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
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                "config/food_values.json",
                FoodValueRegistry::parseFromReader,
                FoodValueRegistry::load,
                "[FoodValueRegistry] Loaded from datapack override",
                "[FoodValueRegistry] Failed to load from datapack, falling back to config folder",
                "[FoodValueRegistry] Loaded from config folder"
        );
    }

    private static boolean registerFoodValueRow(JsonObject obj, int index) {
        com.google.gson.JsonElement categoryEl = obj.get("category");
        if (categoryEl == null || !categoryEl.isJsonPrimitive() || !categoryEl.getAsJsonPrimitive().isString()) {
            Nourished.LOGGER.warn("[FoodValueRegistry] Skipping entry at index {} missing or invalid 'category'", index);
            return false;
        }
        String category = categoryEl.getAsString();
        float protein = MarieJsonUtils.getOptionalFloat(obj, "protein", 0.2f);
        float carbs = MarieJsonUtils.getOptionalFloat(obj, "carbs", 0.2f);
        float fats = MarieJsonUtils.getOptionalFloat(obj, "fats", 0.2f);
        float vitamins = MarieJsonUtils.getOptionalFloat(obj, "vitamins", 0.2f);
        float hydration = MarieJsonUtils.getOptionalFloat(obj, "hydration", 0.2f);
        INSTANCE.registerUnlocked(category, new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
        return true;
    }

    private static void parseFromReader(Reader reader) {
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (arr == null || arr.isEmpty()) {
                Nourished.LOGGER.warn("[FoodValueRegistry] Data was empty, using built-in defaults");
                registerBundledDefaultsUnlocked();
                return;
            }
            for (int i = 0; i < arr.size(); i++) {
                registerFoodValueRow(arr.get(i).getAsJsonObject(), i);
            }
            if (INSTANCE.valuesUnlocked().isEmpty()) {
                Nourished.LOGGER.warn("[FoodValueRegistry] Data was empty, using built-in defaults");
                INSTANCE.resetUnlocked();
                registerBundledDefaultsUnlocked();
            } else {
                INSTANCE.freezeUnlocked();
            }
        });
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
        Objects.requireNonNull(category, "category");
        LinkedHashMap<String, FoodValueDef> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(category, new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            for (Map.Entry<String, FoodValueDef> e : next.entrySet()) {
                INSTANCE.registerUnlocked(e.getKey(), e.getValue());
            }
            INSTANCE.freezeUnlocked();
        });
    }

    private static void parse(Path file) throws IOException {
        JsonArray arr;
        try (Reader r = Files.newBufferedReader(file)) {
            arr = GSON.fromJson(r, JsonArray.class);
        }
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (arr == null) {
                Nourished.LOGGER.warn("[FoodValueRegistry] food_values.json was empty, using built-in defaults");
                registerBundledDefaultsUnlocked();
                return;
            }
            int limit = 2000;
            if (arr.size() > limit) {
                Nourished.LOGGER.warn("[FoodValueRegistry] food_values.json has {} entries, exceeding {} — truncating", arr.size(), limit);
            }
            for (int i = 0; i < Math.min(arr.size(), limit); i++) {
                registerFoodValueRow(arr.get(i).getAsJsonObject(), i);
            }
            if (INSTANCE.valuesUnlocked().isEmpty()) {
                Nourished.LOGGER.warn("[FoodValueRegistry] food_values.json was empty, using built-in defaults");
                INSTANCE.resetUnlocked();
                registerBundledDefaultsUnlocked();
            } else {
                INSTANCE.freezeUnlocked();
            }
        });
    }

    private static void loadDefaults() {
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            registerBundledDefaultsUnlocked();
        });
    }

    private static void registerBundledDefaultsUnlocked() {
        for (FoodValueDef def : loadBundledDefaults()) {
            INSTANCE.registerUnlocked(def.category(), def);
        }
        INSTANCE.freezeUnlocked();
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (FoodValueDef def : loadBundledDefaults()) {
            arr.add(defToJson(def));
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "FoodValueRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (FoodValueDef def : INSTANCE.values()) {
            arr.add(defToJson(def));
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "FoodValueRegistry");
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
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject obj = arr.get(i).getAsJsonObject();
                    com.google.gson.JsonElement categoryEl = obj.get("category");
                    if (categoryEl == null || !categoryEl.isJsonPrimitive() || !categoryEl.getAsJsonPrimitive().isString()) {
                        Nourished.LOGGER.warn("[FoodValueRegistry] Skipping bundled default at index {} missing or invalid 'category'", i);
                        continue;
                    }
                    String category = categoryEl.getAsString();
                    float protein = obj.has("protein") ? obj.get("protein").getAsFloat() : DEFAULT_WEIGHTS[0];
                    float carbs = obj.has("carbs") ? obj.get("carbs").getAsFloat() : DEFAULT_WEIGHTS[1];
                    float fats = obj.has("fats") ? obj.get("fats").getAsFloat() : DEFAULT_WEIGHTS[2];
                    float vitamins = obj.has("vitamins") ? obj.get("vitamins").getAsFloat() : DEFAULT_WEIGHTS[3];
                    float hydration = obj.has("hydration") ? obj.get("hydration").getAsFloat() : DEFAULT_WEIGHTS[4];
                    defaults.add(new FoodValueDef(category, protein, carbs, fats, vitamins, hydration));
                }
            }
        } catch (IOException e) {
            Nourished.LOGGER.warn("[FoodValueRegistry] Failed to load bundled food value defaults: {}", e.getMessage());
        }
        return defaults;
    }
}
