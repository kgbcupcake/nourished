package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieResourceLoader;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads per-item food overrides from config/nourished/overrides/food_overrides.json.
 * This is an escape hatch for modpack creators who want to override specific items.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>Hardcoded Java defaults (empty)</li>
 *   <li>config/nourished/overrides/food_overrides.json</li>
 *   <li>data/nourished/config/food_overrides.json (datapack override)</li>
 * </ol>
 */
@ApiStatus.Internal
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

    private static final class Core extends AbstractRegistry<String, FoodOverride> {
        Core() {
            super("FoodOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    /**
     * Returns the override for an item, if one exists and is enabled.
     */
    public static Optional<FoodOverride> getOverride(String itemId) {
        FoodOverride override = INSTANCE.get(itemId);
        if (override != null && override.enabled()) {
            return Optional.of(override);
        }
        return Optional.empty();
    }

    /**
     * Returns the override for an item (whether enabled or not), or null if none.
     */
    public static FoodOverride get(String itemId) {
        return INSTANCE.get(itemId);
    }

    /** Returns all registered overrides. */
    public static Map<String, FoodOverride> getAll() {
        return INSTANCE.entries();
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path overridesDir = configDir.resolve("overrides");
        Path dataDir = overridesDir.resolve("Overrides");
        Path readmeDir = overridesDir.resolve("Read_Me");
        Path file = dataDir.resolve("food_overrides.json");
        Path oldFile = overridesDir.resolve("food_overrides.json");

        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(readmeDir);
            if (Files.exists(oldFile) && !Files.exists(file)) {
                Files.move(oldFile, file);
            }
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[FoodOverrideRegistry] Wrote default food_overrides.json");
            }
            parse(file);
            Nourished.LOGGER.info("[FoodOverrideRegistry] Loaded {} overrides from config folder", INSTANCE.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[FoodOverrideRegistry] Failed to load food_overrides.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }

        try {
            Path oldReadme = overridesDir.resolve("OVERRIDES_README.md");
            if (Files.exists(oldReadme)) {
                Files.deleteIfExists(oldReadme);
            }
            writeReadmeIfAbsent(readmeDir);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[FoodOverrideRegistry] Failed to write OVERRIDES_README.md", e);
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
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                "config/food_overrides.json",
                FoodOverrideRegistry::parseFromReader,
                FoodOverrideRegistry::load,
                "[FoodOverrideRegistry] Loaded from datapack override",
                "[FoodOverrideRegistry] Failed to load from datapack, falling back to config folder",
                "[FoodOverrideRegistry] Loaded from config folder"
        );
    }

    private static void parseFromReader(Reader reader) {
        INSTANCE.reset();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr != null) {
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

                INSTANCE.register(item, new FoodOverride(item, nutrients, calories, enabled));
            }
        }
        INSTANCE.freeze();
    }

    /**
     * Saves the current registry state back to food_overrides.json.
     */
    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("overrides").resolve("Overrides").resolve("food_overrides.json");
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
        Objects.requireNonNull(item, "item");
        LinkedHashMap<String, FoodOverride> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(item, new FoodOverride(item, new HashMap<>(nutrients), calories, enabled));
        INSTANCE.reset();
        for (Map.Entry<String, FoodOverride> e : next.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
    }

    /**
     * Removes an override.
     */
    public static void removeOverride(String item) {
        Objects.requireNonNull(item, "item");
        LinkedHashMap<String, FoodOverride> next = new LinkedHashMap<>(INSTANCE.entries());
        next.remove(item);
        INSTANCE.reset();
        for (Map.Entry<String, FoodOverride> e : next.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "FoodOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void writeReadmeIfAbsent(Path readmeDir) throws IOException {
        Path readme = readmeDir.resolve("OVERRIDES_README.md");
        if (Files.exists(readme)) {
            return;
        }
        MarieValidation.assertPathUnder(readme, readmeDir, "FoodOverrideRegistry");
        try (InputStream in = FoodOverrideRegistry.class.getResourceAsStream(
                "/data/" + Nourished.MODID + "/config/OVERRIDES_README.md")) {
            if (in == null) {
                Nourished.LOGGER.warn("[FoodOverrideRegistry] No bundled OVERRIDES_README.md, skipping write");
                return;
            }
            Files.copy(in, readme);
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (FoodOverride override : INSTANCE.values()) {
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
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "FoodOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
