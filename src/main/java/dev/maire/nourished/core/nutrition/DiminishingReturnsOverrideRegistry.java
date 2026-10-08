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
 * Loads items the player marked "Diminishing Returns Off" through the in-game item editor's
 * Options page, stored at {@code config/nourished/item_editor/No Diminishing Returns/General/
 * no_diminishing_returns.json} — mirrors {@link NoCaloriesOverrideRegistry}'s file layout and
 * editor-authored-only write path, just for a different per-item flag.
 * <p>
 * Unlike {@code NourishedConfig#enableDiminishingReturns}'s existing mod-wide toggle (which turns
 * the memory-based diminishing-returns multiplier off for every food at once), this flag exempts
 * one specific item from the multiplier while leaving it in effect for everything else — for a
 * food that shouldn't be subject to novelty/repetition penalties at all (e.g. a staple item some
 * other mod expects to be eaten constantly) without disabling the system mod-wide. Checked by
 * {@code NourishedMemoryConfig#serverTrackingMemoryConfig(String)}, which is consulted by
 * MariesLib's {@code SourceApplicationPipeline} once per eat/use with that eat's own source id.
 */
@ApiStatus.Internal
public final class DiminishingReturnsOverrideRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, Boolean> {
        Core() {
            super("DiminishingReturnsOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private DiminishingReturnsOverrideRegistry() {}

    public static boolean isDiminishingReturnsOff(String itemId) {
        return INSTANCE.contains(itemId);
    }

    public static Set<String> getAll() {
        return Set.copyOf(INSTANCE.keys());
    }

    /** Marks an item diminishing-returns-off in memory. Call {@link #save()} to persist it. */
    public static void addDiminishingReturnsOff(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Set<String> next = new LinkedHashSet<>(INSTANCE.keys());
        next.add(itemId);
        replaceAll(next);
    }

    /** Clears an item's diminishing-returns-off flag in memory. Call {@link #save()} to persist it. */
    public static void removeDiminishingReturnsOff(String itemId) {
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
        return itemEditorDir().resolve("No Diminishing Returns").resolve("General").resolve("no_diminishing_returns.json");
    }

    public static void load() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[DiminishingReturnsOverrideRegistry] Loaded {} diminishing-returns-off items", INSTANCE.size());
            } else {
                // Nothing flagged yet: leave No Diminishing Returns/ unwritten rather than creating
                // an empty General/no_diminishing_returns.json placeholder — #save() creates it the
                // first time there's actually something to persist.
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[DiminishingReturnsOverrideRegistry] Failed to load no_diminishing_returns.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[DiminishingReturnsOverrideRegistry] Reloading no_diminishing_returns.json");
        load();
    }

    /** Persists the current in-memory state to {@code item_editor/No Diminishing Returns/General/no_diminishing_returns.json}, creating that folder the first time there's actually something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[DiminishingReturnsOverrideRegistry] Saved no_diminishing_returns.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[DiminishingReturnsOverrideRegistry] Failed to save no_diminishing_returns.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr;
            try {
                arr = GSON.fromJson(r, JsonArray.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[DiminishingReturnsOverrideRegistry] no_diminishing_returns.json failed to parse "
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
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "DiminishingReturnsOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
