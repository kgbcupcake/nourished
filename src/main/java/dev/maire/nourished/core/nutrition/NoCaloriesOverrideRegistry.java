package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieValidation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Loads items the player marked "Calories Off" through the in-game item editor's Options page,
 * stored at {@code config/nourished/item_editor/No Calories/General/no_calories.json} — mirrors
 * {@link ExcludedFoodOverrideRegistry}'s file layout and editor-authored-only write path, just for
 * a different per-item flag.
 * <p>
 * Unlike {@link ExcludedFoodOverrideRegistry} (which drops an item out of nutrient classification
 * entirely), this flag leaves nutrient application untouched — the item still fills nutrient bars
 * normally — and only zeroes its calorie contribution. Checked by {@code
 * NourishedContextBuilder}'s {@code sourceDeltaResolver}, which is where the calorie number
 * actually originates ({@link dev.maire.nourished.core.nutrition.FoodNutritionRegistry#computeDietDelta}
 * or a {@link FoodOverrideRegistry.FoodOverride}'s own {@code calories()}) — MariesLib's {@code
 * SourceApplicationPipeline} applies that number straight to {@code TrackingData.total} before
 * Nourished's own food-eaten handling ever runs, so zeroing it anywhere downstream of that (e.g.
 * only suppressing the Calories-tracker increment) is too late: the total, the per-meal "you
 * consumed N calories" notification and the Calorie HUD/history all trace back to this same
 * source number.
 */
@ApiStatus.Internal
public final class NoCaloriesOverrideRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, Boolean> {
        Core() {
            super("NoCaloriesOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private NoCaloriesOverrideRegistry() {}

    public static boolean isCaloriesOff(String itemId) {
        return INSTANCE.contains(itemId);
    }

    public static Set<String> getAll() {
        return Set.copyOf(INSTANCE.keys());
    }

    /** Marks an item calories-off in memory. Call {@link #save()} to persist it. */
    public static void addCaloriesOff(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Set<String> next = new LinkedHashSet<>(INSTANCE.keys());
        next.add(itemId);
        replaceAll(next);
    }

    /** Clears an item's calories-off flag in memory. Call {@link #save()} to persist it. */
    public static void removeCaloriesOff(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Set<String> next = new LinkedHashSet<>(INSTANCE.keys());
        next.remove(itemId);
        replaceAll(next);
    }

    private static void replaceAll(Set<String> items) {
        INSTANCE.reset();
        for (String item : items) {
            INSTANCE.register(item, Boolean.TRUE);
        }
        INSTANCE.freeze();
    }

    private static Path itemEditorDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("item_editor");
    }

    private static Path file() {
        return itemEditorDir().resolve("No Calories").resolve("General").resolve("no_calories.json");
    }

    public static void load() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[NoCaloriesOverrideRegistry] Loaded {} calories-off items", INSTANCE.size());
            } else {
                // Nothing flagged yet: leave No Calories/ unwritten rather than creating an empty
                // General/no_calories.json placeholder — #save() creates it the first time there's
                // actually something to persist.
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[NoCaloriesOverrideRegistry] Failed to load no_calories.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[NoCaloriesOverrideRegistry] Reloading no_calories.json");
        load();
    }

    /** Persists the current in-memory state to {@code item_editor/No Calories/General/no_calories.json}, creating that folder the first time there's actually something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[NoCaloriesOverrideRegistry] Saved no_calories.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[NoCaloriesOverrideRegistry] Failed to save no_calories.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr;
            try {
                arr = GSON.fromJson(r, JsonArray.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[NoCaloriesOverrideRegistry] no_calories.json failed to parse "
                        + "(check for trailing commas / invalid JSON); keeping previously loaded state", e);
                throw e;
            }
            INSTANCE.reset();
            if (arr != null) {
                for (JsonElement el : arr) {
                    if (el != null && el.isJsonPrimitive()) {
                        INSTANCE.register(el.getAsString(), Boolean.TRUE);
                    }
                }
            }
            INSTANCE.freeze();
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (String item : INSTANCE.keys()) {
            arr.add(item);
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "NoCaloriesOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
