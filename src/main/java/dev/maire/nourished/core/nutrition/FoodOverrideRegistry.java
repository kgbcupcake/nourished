package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.marie.MariesLib.registry.AbstractRegistry;
import dev.marie.MariesLib.util.MarieResourceLoader;
import dev.marie.MariesLib.util.MarieValidation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
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
        Path file = configDir.resolve("food_overrides.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[FoodOverrideRegistry] Wrote default food_overrides.json");
            }
            writeReadmeIfAbsent(configDir);
            parse(file);
            Nourished.LOGGER.info("[FoodOverrideRegistry] Loaded {} overrides from config folder", INSTANCE.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[FoodOverrideRegistry] Failed to load food_overrides.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
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

    private static void writeReadmeIfAbsent(Path configDir) throws IOException {
        Path readme = configDir.resolve("OVERRIDES_README.md");
        if (Files.exists(readme)) {
            return;
        }
        MarieValidation.assertPathUnder(readme, configDir, "FoodOverrideRegistry");
        String content = """
                # Nourished — Food Overrides

                `food_overrides.json` lets you override nutrient values and calories for any \
                specific item, regardless of how Nourished classified it elsewhere.

                ## Schema

                ```json
                [
                  {
                    "item": "minecraft:steak",
                    "nutrients": {
                      "proteins": 0.8,
                      "fats": 0.2
                    },
                    "calories": 60,
                    "enabled": true
                  }
                ]
                ```

                - `item` — the item's registry id (e.g. `minecraft:steak`, `farmersdelight:onion`)
                - `nutrients` — nutrient key to weight (matches the keys shown in `/marieslib status` \
                or your registered nutrients)
                - `calories` — integer calorie value for this item
                - `enabled` — set to `false` to disable an override without deleting it

                ## Getting starting values

                Run `/marieslib dump nourished_nutrients` (or use the "Export All Foods" button in \
                the Scanner tab of the config screen) to write \
                `nourished_nutrients_export/` — a folder of read-only reference files, one per \
                nutrient category (`fruits.json`, `proteins.json`, `vegetables.json`, etc.), each \
                listing every item Nourished currently resolves into that category, with its live \
                nutrient values and calories.

                To turn an export entry into an override, copy it from the relevant category file \
                into `food_overrides.json` and reshape it from:

                ```json
                { "item": "minecraft:steak", "nutrients": { "proteins": 0.8 }, "calories": 60 }
                ```

                to:

                ```json
                { "item": "minecraft:steak", "nutrients": { "proteins": 0.8 }, "calories": 60, "enabled": true }
                ```

                (the export entries already include the `item` field — just add `"enabled": true`)

                The export files are reference only — editing them does nothing on their own. Only \
                entries actually present in `food_overrides.json` take effect.
                """.stripIndent();
        try (Writer w = Files.newBufferedWriter(readme)) {
            w.write(content);
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
