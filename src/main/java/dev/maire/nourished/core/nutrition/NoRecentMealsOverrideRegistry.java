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
 * Loads items the player marked "Exclude from Recent Meals" through the in-game item editor's
 * Options page, stored at {@code config/nourished/item_editor/No Recent Meals/General/no_recent_meals.json}
 * — mirrors {@link ExcludedFoodOverrideRegistry}'s file layout and editor-authored-only write path,
 * just for a different per-item flag.
 * <p>
 * Nutrients and calories apply exactly as normal for a flagged item; the only thing suppressed is
 * the {@link dev.maire.nourished.core.diet.DietAttachment#recordRecentMeal} call that {@link
 * dev.maire.nourished.core.handler.NourishedFoodTriggerHandler} would otherwise make for it, so it
 * never appears in the Diet Screen's Recent Meals list.
 */
@ApiStatus.Internal
public final class NoRecentMealsOverrideRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, Boolean> {
        Core() {
            super("NoRecentMealsOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private NoRecentMealsOverrideRegistry() {}

    public static boolean isExcluded(String itemId) {
        return INSTANCE.contains(itemId);
    }

    public static Set<String> getAll() {
        return Set.copyOf(INSTANCE.keys());
    }

    /** Marks an item excluded from Recent Meals in memory. Call {@link #save()} to persist it. */
    public static void addExcluded(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Set<String> next = new LinkedHashSet<>(INSTANCE.keys());
        next.add(itemId);
        replaceAll(next);
    }

    /** Clears an item's Recent Meals exclusion in memory. Call {@link #save()} to persist it. */
    public static void removeExcluded(String itemId) {
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
        return itemEditorDir().resolve("No Recent Meals").resolve("General").resolve("no_recent_meals.json");
    }

    public static void load() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[NoRecentMealsOverrideRegistry] Loaded {} items excluded from Recent Meals", INSTANCE.size());
            } else {
                // Nothing flagged yet: leave No Recent Meals/ unwritten rather than creating an
                // empty General/no_recent_meals.json placeholder — #save() creates it the first time
                // there's actually something to persist.
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[NoRecentMealsOverrideRegistry] Failed to load no_recent_meals.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[NoRecentMealsOverrideRegistry] Reloading no_recent_meals.json");
        load();
    }

    /** Persists the current in-memory state to {@code item_editor/No Recent Meals/General/no_recent_meals.json}, creating that folder the first time there's actually something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[NoRecentMealsOverrideRegistry] Saved no_recent_meals.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[NoRecentMealsOverrideRegistry] Failed to save no_recent_meals.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr;
            try {
                arr = GSON.fromJson(r, JsonArray.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[NoRecentMealsOverrideRegistry] no_recent_meals.json failed to parse "
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
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "NoRecentMealsOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
