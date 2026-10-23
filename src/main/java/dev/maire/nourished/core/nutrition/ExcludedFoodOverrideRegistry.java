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
 * Loads items the player excluded from nutrient classification through the in-game item editor,
 * stored at {@code config/nourished/item_editor/Excluded Foods/General/excluded_foods.json} — its
 * own {@code General} subfolder, matching MariesLib's {@code SourceClassificationRegistry} pattern
 * for a category folder that (so far) only ever holds one file; nothing sits loose anywhere in this
 * tree, not even one level inside {@code Excluded Foods/} itself.
 * <p>
 * This is a separate layer from MariesLib's {@code ExcludedItemsRegistry}
 * ({@code config/nourished/overrides/Overrides/excluded_items.json}), which is the modpack-author
 * escape hatch edited by hand. This registry is the editor-authored layer: it's written by Save
 * in the item editor UI and is never touched by modpack creators directly. Both layers are
 * checked by {@link NutrientClassificationLookup}.
 */
@ApiStatus.Internal
public final class ExcludedFoodOverrideRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, Boolean> {
        Core() {
            super("ExcludedFoodOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ExcludedFoodOverrideRegistry() {}

    public static boolean isExcluded(String itemId) {
        return INSTANCE.contains(itemId);
    }

    public static Set<String> getAll() {
        return Set.copyOf(INSTANCE.keys());
    }

    /** Adds an editor-authored exclusion in memory. Call {@link #save()} to persist it. */
    public static void addExcluded(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Set<String> next = new LinkedHashSet<>(INSTANCE.keys());
        next.add(itemId);
        replaceAll(next);
    }

    /** Removes an editor-authored exclusion in memory. Call {@link #save()} to persist it. */
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
        return itemEditorDir().resolve("Excluded Foods").resolve("General").resolve("excluded_foods.json");
    }

    public static void load() {
        Path file = file();
        try {
            // Earlier layouts of this file, both migrated below like any other old location: briefly
            // loose directly under item_editor/, then loose directly under Excluded Foods/ (nothing
            // should sit loose anywhere in this tree, including one level inside a category folder
            // that only ever holds the one file).
            Path oldLooseFile = itemEditorDir().resolve("excluded_foods.json");
            if (Files.exists(oldLooseFile) && !Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.move(oldLooseFile, file);
            }
            Files.deleteIfExists(oldLooseFile);
            Path oldCategoryLooseFile = itemEditorDir().resolve("Excluded Foods").resolve("excluded_foods.json");
            if (Files.exists(oldCategoryLooseFile) && !Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.move(oldCategoryLooseFile, file);
            }
            Files.deleteIfExists(oldCategoryLooseFile);
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[ExcludedFoodOverrideRegistry] Loaded {} editor-excluded items", INSTANCE.size());
            } else {
                // Nothing excluded yet: leave Excluded Foods/ unwritten rather than creating an
                // empty General/excluded_foods.json placeholder — #save() creates it the first time
                // there's actually something to persist.
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[ExcludedFoodOverrideRegistry] Failed to load excluded_foods.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[ExcludedFoodOverrideRegistry] Reloading excluded_foods.json");
        load();
    }

    /** Persists the current in-memory state to {@code item_editor/Excluded Foods/General/excluded_foods.json}, creating that folder the first time there's actually something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[ExcludedFoodOverrideRegistry] Saved excluded_foods.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[ExcludedFoodOverrideRegistry] Failed to save excluded_foods.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr;
            try {
                arr = GSON.fromJson(r, JsonArray.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[ExcludedFoodOverrideRegistry] excluded_foods.json failed to parse "
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
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "ExcludedFoodOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
