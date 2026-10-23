package dev.maire.nourished.modules.RawFood.core;

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
 * Loads per-item raw-food severity overrides the player set through the in-game item editor's
 * Options page, stored at {@code config/nourished/item_editor/Raw Food Tier/General/raw_food_tier_overrides.json}
 * — mirrors {@link dev.maire.nourished.core.nutrition.ExcludedFoodOverrideRegistry}'s file
 * layout/editor-authored-only write path, just keyed to a {@link RawSeverity} value per item
 * instead of a boolean flag.
 * <p>
 * Forces {@link dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier#classify} to this
 * severity instead of whatever tags/cookedness/tokens would otherwise resolve — for modded raw
 * foods that get misclassified by those heuristics.
 */
@ApiStatus.Internal
public final class RawFoodTierOverrideRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, RawSeverity> {
        Core() {
            super("RawFoodTierOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private RawFoodTierOverrideRegistry() {}

    @Nullable
    public static RawSeverity getOverride(String itemId) {
        return INSTANCE.get(itemId);
    }

    public static Map<String, RawSeverity> getAll() {
        return Map.copyOf(INSTANCE.entries());
    }

    /** Sets an item's raw-food tier override in memory. Call {@link #save()} to persist it. */
    public static void setOverride(String itemId, RawSeverity severity) {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(severity, "severity");
        Map<String, RawSeverity> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(itemId, severity);
        replaceAll(next);
    }

    /** Clears an item's raw-food tier override in memory. Call {@link #save()} to persist it. */
    public static void removeOverride(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        Map<String, RawSeverity> next = new LinkedHashMap<>(INSTANCE.entries());
        next.remove(itemId);
        replaceAll(next);
    }

    private static void replaceAll(Map<String, RawSeverity> items) {
        INSTANCE.reset();
        for (Map.Entry<String, RawSeverity> e : items.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
    }

    private static Path itemEditorDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("item_editor");
    }

    private static Path file() {
        return itemEditorDir().resolve("Raw Food Tier").resolve("General").resolve("raw_food_tier_overrides.json");
    }

    public static void load() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[RawFoodTierOverrideRegistry] Loaded {} raw food tier overrides", INSTANCE.size());
            } else {
                // Nothing overridden yet: leave Raw Food Tier/ unwritten rather than creating an
                // empty General/raw_food_tier_overrides.json placeholder — #save() creates it the
                // first time there's actually something to persist.
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[RawFoodTierOverrideRegistry] Failed to load raw_food_tier_overrides.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[RawFoodTierOverrideRegistry] Reloading raw_food_tier_overrides.json");
        load();
    }

    /** Persists the current in-memory state to {@code item_editor/Raw Food Tier/General/raw_food_tier_overrides.json}, creating that folder the first time there's actually something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[RawFoodTierOverrideRegistry] Saved raw_food_tier_overrides.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[RawFoodTierOverrideRegistry] Failed to save raw_food_tier_overrides.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj;
            try {
                obj = GSON.fromJson(r, JsonObject.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[RawFoodTierOverrideRegistry] raw_food_tier_overrides.json failed to parse "
                        + "(check for trailing commas / invalid JSON); keeping previously loaded state", e);
                throw e;
            }
            INSTANCE.reset();
            if (obj != null) {
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    RawSeverity severity = parseSeverity(entry.getValue().getAsString());
                    if (severity != null) {
                        INSTANCE.register(entry.getKey(), severity);
                    } else {
                        Nourished.LOGGER.warn("[RawFoodTierOverrideRegistry] Skipping unknown severity '{}' for '{}'",
                                entry.getValue().getAsString(), entry.getKey());
                    }
                }
            }
            INSTANCE.freeze();
        }
    }

    @Nullable
    private static RawSeverity parseSeverity(String name) {
        try {
            return RawSeverity.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, RawSeverity> e : INSTANCE.entries().entrySet()) {
            obj.addProperty(e.getKey(), e.getValue().name());
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "RawFoodTierOverrideRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(obj, w);
        }
    }
}
