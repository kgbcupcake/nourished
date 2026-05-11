package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads bundled {@code data/nourished/config/mod_compat.json} at startup (integration metadata and
 * optional per-mod tuning). Other systems may query {@link #isLoaded(String)} or meal thresholds.
 */
@ApiStatus.Internal
public final class ModCompatRegistry {

    private static final Gson GSON = new Gson();
    private static final String RESOURCE_PATH = "/data/nourished/config/mod_compat.json";
    private static final String SOLONION_MOD_ID = "solonion";

    private static final Map<String, IntegrationEntry> INTEGRATIONS = new LinkedHashMap<>();

    private ModCompatRegistry() {}

    public record IntegrationEntry(String modId, Integer heavyMealNutritionThreshold, String notes) {}

    public static void load() {
        INTEGRATIONS.clear();
        try (InputStream in = ModCompatRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                Nourished.LOGGER.warn("[ModCompatRegistry] Missing resource {}", RESOURCE_PATH);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("integrations") || !root.get("integrations").isJsonArray()) {
                    Nourished.LOGGER.warn("[ModCompatRegistry] mod_compat.json missing integrations array");
                    return;
                }
                JsonArray arr = root.getAsJsonArray("integrations");
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = el.getAsJsonObject();
                    if (!o.has("modId")) {
                        continue;
                    }
                    String modId = o.get("modId").getAsString();
                    Integer heavy = null;
                    if (o.has("heavyMealNutritionThreshold") && !o.get("heavyMealNutritionThreshold").isJsonNull()) {
                        heavy = Mth.clamp(o.get("heavyMealNutritionThreshold").getAsInt(), 1, 20);
                    }
                    String notes = "";
                    if (o.has("notes") && o.get("notes").isJsonPrimitive()) {
                        notes = o.get("notes").getAsString();
                    }
                    INTEGRATIONS.put(modId, new IntegrationEntry(modId, heavy, notes));
                }
            }
            Nourished.LOGGER.info("[ModCompatRegistry] Loaded {} integration entries from mod_compat.json",
                    INTEGRATIONS.size());
        } catch (Exception e) {
            Nourished.LOGGER.error("[ModCompatRegistry] Failed to load mod_compat.json", e);
            INTEGRATIONS.clear();
        }
    }

    /** @return immutable view of parsed integration rows */
    public static Map<String, IntegrationEntry> integrations() {
        return Collections.unmodifiableMap(INTEGRATIONS);
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * When Spice of Life: Onion ({@code solonion}) is loaded, returns {@code heavyMealNutritionThreshold}
     * from its mod_compat entry when present; otherwise {@link ModuleCache#heavyMealNutritionThreshold}.
     * When {@code solonion} is not loaded, returns {@link ModuleCache#heavyMealNutritionThreshold} only.
     */
    public static int getHeavyMealThreshold() {
        if (!ModList.get().isLoaded(SOLONION_MOD_ID)) {
            return ModuleCache.heavyMealNutritionThreshold;
        }
        IntegrationEntry entry = INTEGRATIONS.get(SOLONION_MOD_ID);
        if (entry != null && entry.heavyMealNutritionThreshold != null) {
            return entry.heavyMealNutritionThreshold();
        }
        return ModuleCache.heavyMealNutritionThreshold;
    }
}
