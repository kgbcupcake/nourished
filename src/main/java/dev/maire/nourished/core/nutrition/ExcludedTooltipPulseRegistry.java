package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-item "pulse" flag for the excluded-item tooltip line, set from the item editor's Exclude
 * Tooltip page. MariesLib's {@code TooltipColorRegistry}/{@code TooltipMessageRegistry} have no
 * concept of animation (tooltip text is plain {@code Style}-colored {@code Component}s), so unlike
 * the message/color overrides in {@link ExcludedTooltipOverrides} (which write straight into those
 * registries' own files), the pulse flag and its speed live here instead, applied client-side in
 * {@code ClientEvents#onItemTooltip} by recoloring the line each frame.
 *
 * <p>Stored at {@code config/nourished/item_editor/Excluded Tooltip/General/excluded_tooltip_pulse.json},
 * following {@link ExcludedFoodOverrideRegistry}'s folder convention.
 */
@ApiStatus.Internal
public final class ExcludedTooltipPulseRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum Speed { SLOW, NORMAL, FAST }

    private static final class Core extends AbstractRegistry<String, String> {
        Core() {
            super("ExcludedTooltipPulseRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ExcludedTooltipPulseRegistry() {}

    public static boolean isEnabled(String itemId) {
        return INSTANCE.contains(itemId);
    }

    public static Speed getSpeed(String itemId) {
        String stored = INSTANCE.get(itemId);
        if (stored == null) {
            return Speed.NORMAL;
        }
        try {
            return Speed.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return Speed.NORMAL;
        }
    }

    /** Sets the in-memory pulse state for {@code itemId}. Call {@link #save()} to persist it. */
    public static void set(String itemId, boolean enabled, Speed speed) {
        Map<String, String> next = new LinkedHashMap<>(INSTANCE.entries());
        if (enabled) {
            next.put(itemId, speed.name());
        } else {
            next.remove(itemId);
        }
        replaceAll(next);
    }

    private static void replaceAll(Map<String, String> entries) {
        INSTANCE.reset();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            INSTANCE.register(entry.getKey(), entry.getValue());
        }
        INSTANCE.freeze();
    }

    private static Path itemEditorDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("item_editor");
    }

    private static Path file() {
        return itemEditorDir().resolve("Excluded Tooltip").resolve("General").resolve("excluded_tooltip_pulse.json");
    }

    public static void load() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                parse(file);
                Nourished.LOGGER.info("[ExcludedTooltipPulseRegistry] Loaded {} pulse overrides", INSTANCE.size());
            } else {
                INSTANCE.reset();
                INSTANCE.freeze();
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[ExcludedTooltipPulseRegistry] Failed to load excluded_tooltip_pulse.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        load();
    }

    /** Persists the current in-memory state, creating {@code Excluded Tooltip/General/} the first time there's something to write. */
    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            writeRegistry(file);
            Nourished.LOGGER.info("[ExcludedTooltipPulseRegistry] Saved excluded_tooltip_pulse.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[ExcludedTooltipPulseRegistry] Failed to save excluded_tooltip_pulse.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj;
            try {
                obj = GSON.fromJson(r, JsonObject.class);
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[ExcludedTooltipPulseRegistry] excluded_tooltip_pulse.json failed to parse "
                        + "(check for trailing commas / invalid JSON); keeping previously loaded state", e);
                throw e;
            }
            INSTANCE.reset();
            if (obj != null) {
                for (String key : obj.keySet()) {
                    var el = obj.get(key);
                    if (el != null && el.isJsonPrimitive()) {
                        INSTANCE.register(key, el.getAsString());
                    }
                }
            }
            INSTANCE.freeze();
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonObject obj = new JsonObject();
        for (String key : INSTANCE.keys()) {
            obj.addProperty(key, INSTANCE.get(key));
        }
        MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "ExcludedTooltipPulseRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(obj, w);
        }
    }
}
