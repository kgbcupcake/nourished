package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieValidation;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads per-item fixed-calorie overrides the player set through the in-game item editor's Options
 * page ("Use Default Calculation" off, with a custom "Calories" value), stored at {@code
 * config/nourished/item_editor/Custom Calories/General/custom_calories.json} — mirrors {@link
 * ExcludedFoodOverrideRegistry}'s file layout and editor-authored-only write path, just keyed to an
 * {@code int} per item instead of a boolean flag.
 * <p>
 * Unlike {@link NoCaloriesOverrideRegistry} (which zeroes an item's calorie contribution entirely),
 * this substitutes a fixed number for whatever {@link FoodNutritionRegistry#computeDietDelta} or a
 * {@link FoodOverrideRegistry.FoodOverride} would otherwise compute — for a modded food whose
 * automatic calorie formula doesn't match what it should actually be worth. Checked by {@code
 * NourishedContextBuilder}'s {@code sourceDeltaResolver} ahead of the normal calculation, after the
 * "Calories Off" check (an item flagged both ways is zeroed — off wins).
 */
@ApiStatus.Internal
public final class CustomCaloriesOverrideRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, Integer> {
        Core() {
            super("CustomCaloriesOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private CustomCaloriesOverrideRegistry() {}

    @Nullable
    public static Integer getOverride(String itemId) {
        return INSTANCE.get(itemId);
    }

    public static Map<String, Integer> getAll() {
        return Map.copyOf(INSTANCE.entries());
    }

    /** Sets an item's custom calorie value in memory. Call {@link #save()} to persist it. */
    public static void setOverride(String itemId, int calories) {
        Objects.requireNonNull(itemId, "itemId");
        Map<String, Integer> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(itemId, calories);
        replaceAll(next);
    }

    /** Clears an item's custom calorie value in memory. Call {@link #save()} to persist it. */
    public static void removeOverride(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Map<String, Integer> next = new LinkedHashMap<>(INSTANCE.entries());
        next.remove(itemId);
        replaceAll(next);
    }

    private static void replaceAll(Map<String, Integer> items) {
        INSTANCE.reset();
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
    }

    private static Path itemEditorDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("item_editor");
    }

    private static Path file() {
        return itemEditorDir().resolve("Custom Calories").resolve("General").resolve("custom_calories.json");
    }

    public static void load() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[CustomCaloriesOverrideRegistry] Loaded {} custom calorie overrides", INSTANCE.size());
            } else {
                // Nothing overridden yet: leave Custom Calories/ unwritten rather than creating an
                // empty General/custom_calories.json placeholder — #save() creates it the first time
                // there's actually something to persist.
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[CustomCaloriesOverrideRegistry] Failed to load custom_calories.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[CustomCaloriesOverrideRegistry] Reloading custom_calories.json");
        load();
    }

    /** Persists the current in-memory state to {@code item_editor/Custom Calories/General/custom_calories.json}, creating that folder the first time there's actually something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[CustomCaloriesOverrideRegistry] Saved custom_calories.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[CustomCaloriesOverrideRegistry] Failed to save custom_calories.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj;
            try {
                obj = GSON.fromJson(r, JsonObject.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[CustomCaloriesOverrideRegistry] custom_calories.json failed to parse "
                        + "(check for trailing commas / invalid JSON); keeping previously loaded state", e);
                throw e;
            }
            INSTANCE.reset();
            if (obj != null) {
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    try {
                        INSTANCE.register(entry.getKey(), entry.getValue().getAsInt());
                    } catch (RuntimeException e) {
                        Nourished.LOGGER.warn("[CustomCaloriesOverrideRegistry] Skipping malformed entry for '{}': {}",
                                entry.getKey(), e.getMessage());
                    }
                }
            }
            INSTANCE.freeze();
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Integer> e : INSTANCE.entries().entrySet()) {
            obj.addProperty(e.getKey(), e.getValue());
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "CustomCaloriesOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(obj, w);
        }
    }
}
