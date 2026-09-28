package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.api.ApiStatus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-nutrient decay rate / critical threshold overrides, stored in nutrient_overrides.json so any
 * nutrient key works — including addon nutrients registered after the TOML spec was built.
 */
@ApiStatus.Internal
public final class NutrientOverrideStore {

    public static final double UNSET = -1.0d;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Double> DECAY = new LinkedHashMap<>();
    private static final Map<String, Double> CRITICAL = new LinkedHashMap<>();
    private static boolean loaded;

    private NutrientOverrideStore() {}

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("nutrient_overrides.json");
    }

    /** Loads the JSON file and moves any legacy TOML overrides into it. Call once config is loaded. */
    public static synchronized void init(NourishedConfig config) {
        ensureLoaded();
        boolean migrated = migrate(config.nutrientDecayRateOverrides(), DECAY)
                | migrate(config.nutrientCriticalThresholdOverrides(), CRITICAL);
        if (migrated) {
            Nourished.LOGGER.info("[NutrientOverrideStore] Moved per-nutrient overrides from nourished-common.toml to nutrient_overrides.json");
            save();
            NourishedConfig.saveNow();
        }
    }

    public static synchronized double decayRate(String key) {
        ensureLoaded();
        return DECAY.getOrDefault(key, UNSET);
    }

    public static synchronized double criticalThreshold(String key) {
        ensureLoaded();
        return CRITICAL.getOrDefault(key, UNSET);
    }

    public static synchronized void setDecayRate(String key, double value) {
        ensureLoaded();
        put(DECAY, key, value);
    }

    public static synchronized void setCriticalThreshold(String key, double value) {
        ensureLoaded();
        put(CRITICAL, key, value);
    }

    public static synchronized Map<String, Double> decayRates() {
        ensureLoaded();
        return Collections.unmodifiableMap(new LinkedHashMap<>(DECAY));
    }

    public static synchronized Map<String, Double> criticalThresholds() {
        ensureLoaded();
        return Collections.unmodifiableMap(new LinkedHashMap<>(CRITICAL));
    }

    public static synchronized void save() {
        Map<String, JsonObject> byKey = new TreeMap<>();
        DECAY.forEach((k, v) -> byKey.computeIfAbsent(k, x -> new JsonObject()).addProperty("decayRate", v));
        CRITICAL.forEach((k, v) -> byKey.computeIfAbsent(k, x -> new JsonObject()).addProperty("criticalThreshold", v));
        JsonObject root = new JsonObject();
        byKey.forEach(root::add);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException e) {
            Nourished.LOGGER.error("[NutrientOverrideStore] Failed to write {}", file(), e);
        }
    }

    private static void put(Map<String, Double> map, String key, double value) {
        if (value < 0d || !Double.isFinite(value)) {
            map.remove(key);
        } else {
            map.put(key, Math.min(1d, value));
        }
    }

    private static boolean migrate(Map<String, ModConfigSpec.DoubleValue> legacy, Map<String, Double> target) {
        boolean changed = false;
        for (Map.Entry<String, ModConfigSpec.DoubleValue> entry : legacy.entrySet()) {
            double v = entry.getValue().get();
            if (v < 0d) {
                continue;
            }
            target.putIfAbsent(entry.getKey(), v);
            // Clear the TOML copy so the JSON file is the only source from here on.
            entry.getValue().set(UNSET);
            changed = true;
        }
        return changed;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = file();
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (root == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject o = entry.getValue().getAsJsonObject();
                if (o.has("decayRate")) put(DECAY, entry.getKey(), o.get("decayRate").getAsDouble());
                if (o.has("criticalThreshold")) put(CRITICAL, entry.getKey(), o.get("criticalThreshold").getAsDouble());
            }
        } catch (Exception e) {
            Nourished.LOGGER.error("[NutrientOverrideStore] Failed to read {}, ignoring it", file, e);
        }
    }
}
