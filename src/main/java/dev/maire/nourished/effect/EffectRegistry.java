package dev.maire.nourished.effect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.nutrition.Nourished;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loads effect definitions from config/nourished/effects.json.
 * Writes a default file on first run. Call {@link #load()} after
 * {@link dev.maire.nourished.nutrition.NutrientRegistry#load()}.
 */
@ApiStatus.Internal
public class EffectRegistry {

    public record EffectDef(
            String id,
            String effect,
            String nutrient,
            String trigger,
            double threshold,
            int amplifier,
            int durationTicks,
            boolean enabled,
            double thresholdMax,
            boolean ambient,
            boolean showParticles
    ) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // volatile: replaced atomically after each full parse/save (reload thread); read concurrently from server tick.
    private static volatile List<EffectDef> REGISTRY = List.of();
    private static final Set<String> previousEffectIds = new HashSet<>();

    private static final String[] DEFAULT_EFFECT_RESOURCES = {
            "protein_penalty",
            "carbs_penalty",
            "vitamins_penalty",
            "hydration_penalty",
            "all_high_health",
            "all_high_regen"
    };

    public static List<EffectDef> getAll() {
        return REGISTRY;
    }

    /**
     * Registers an externally-defined effect via the public API.
     * Called by {@link dev.maire.nourished.api.NourishedAPI#registerCustomEffect}.
     */
    public static void registerExternal(dev.maire.nourished.api.EffectDefinition definition) {
        String trigger = switch (definition.getThresholdType()) {
            case CRITICAL -> "below";
            case LOW -> "below";
            case EXCESS -> "above";
            case BONUS -> "all_above";
        };
        EffectDef def = new EffectDef(
                "api_" + definition.getNutrientKey() + "_" + definition.getEffectId().getPath(),
                definition.getEffectId().toString(),
                definition.getNutrientKey(),
                trigger,
                definition.getThreshold(),
                definition.getAmplifier(),
                definition.getDuration(),
                true,
                1.0,
                true,
                false
        );
        List<EffectDef> next = new ArrayList<>(REGISTRY);
        next.add(def);
        REGISTRY = List.copyOf(next);
        Nourished.LOGGER.info("[EffectRegistry] Registered external effect: {}", def.id());
    }

    public static Set<String> getPreviousEffectIds() {
        return Collections.unmodifiableSet(previousEffectIds);
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("effects.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[EffectRegistry] Wrote default effects.json");
            }
            parse(file);
            Nourished.LOGGER.info("[EffectRegistry] Loaded {} effects from {}", REGISTRY.size(), file);
        } catch (IOException e) {
            Nourished.LOGGER.error("[EffectRegistry] Failed to load effects.json, using built-in defaults", e);
            loadDefaults();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[EffectRegistry] Reloading effects.json");
        load();
        EffectConflictDetector.clearWarned();
        NutritionEffectApplier.clearWarnedPlayers();
    }

    /**
     * Attempts to load from datapack first, then falls back to config folder.
     * Call this from a reload listener when datapacks are available.
     */
    public static void loadFromDatapack(ResourceManager resourceManager) {
        ResourceLocation datapackPath = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "config/effects.json");
        Optional<Resource> resource = resourceManager.getResource(datapackPath);

        if (resource.isPresent()) {
            try (InputStream is = resource.get().open();
                 Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                parseFromReader(r);
                Nourished.LOGGER.info("[EffectRegistry] Loaded from datapack override");
                return;
            } catch (IOException e) {
                Nourished.LOGGER.error("[EffectRegistry] Failed to load from datapack, falling back to config folder", e);
            }
        }

        load();
        Nourished.LOGGER.info("[EffectRegistry] Loaded from config folder");
    }

    private static void parseJson(JsonArray arr, List<EffectDef> target) {
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String effect = obj.get("effect").getAsString();
            String nutrient = obj.get("nutrient").getAsString();
            String trigger = obj.get("trigger").getAsString();
            if (!obj.has("threshold") || !obj.has("amplifier") || !obj.has("duration_ticks")) {
                Nourished.LOGGER.warn("[EffectRegistry] Skipping effect '{}' missing required numeric fields", id);
                continue;
            }
            double threshold = obj.get("threshold").getAsDouble();
            int amplifier = obj.get("amplifier").getAsInt();
            int durationTicks = obj.get("duration_ticks").getAsInt();
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            double thresholdMax = obj.has("threshold_max") ? obj.get("threshold_max").getAsDouble() : 1.0;
            boolean ambient = !obj.has("ambient") || obj.get("ambient").getAsBoolean();
            boolean showParticles = obj.has("show_particles") && obj.get("show_particles").getAsBoolean();
            target.add(new EffectDef(id, effect, nutrient, trigger, threshold, amplifier, durationTicks, enabled, thresholdMax, ambient, showParticles));
        }
    }

    private static void parseFromReader(Reader reader) {
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr == null || arr.isEmpty()) {
            Nourished.LOGGER.warn("[EffectRegistry] Data was empty, using built-in defaults");
            loadDefaults();
            return;
        }
        List<EffectDef> old = REGISTRY;
        previousEffectIds.clear();
        for (EffectDef def : old) {
            previousEffectIds.add(def.effect());
        }
        List<EffectDef> next = new ArrayList<>();
        parseJson(arr, next);
        if (next.isEmpty()) {
            loadDefaults();
        } else {
            REGISTRY = List.copyOf(next);
        }
    }

    private static void parse(Path file) throws IOException {
        List<EffectDef> old = REGISTRY;
        previousEffectIds.clear();
        for (EffectDef def : old) {
            previousEffectIds.add(def.effect());
        }
        List<EffectDef> next = new ArrayList<>();
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            if (arr != null) {
                parseJson(arr, next);
            }
        }
        if (next.isEmpty()) {
            Nourished.LOGGER.warn("[EffectRegistry] effects.json was empty, using built-in defaults");
            loadDefaults();
        } else {
            REGISTRY = List.copyOf(next);
        }
    }

    private static void loadDefaults() {
        List<EffectDef> old = REGISTRY;
        previousEffectIds.clear();
        for (EffectDef def : old) {
            previousEffectIds.add(def.effect());
        }
        List<EffectDef> next = new ArrayList<>(loadBundledDefaults());
        REGISTRY = List.copyOf(next);
    }

    private static void writeDefaults(Path file) throws IOException {
        writeDefinitionsToPath(file, loadBundledDefaults());
    }

    /**
     * Writes the given definitions to {@code config/nourished/effects.json} and replaces the in-memory registry.
     */
    public static void saveAll(List<EffectDef> definitions) throws IOException {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("effects.json");
        Files.createDirectories(configDir);
        writeDefinitionsToPath(file, definitions);
        REGISTRY = List.copyOf(definitions);
    }

    private static void writeDefinitionsToPath(Path file, List<EffectDef> definitions) throws IOException {
        JsonArray arr = new JsonArray();
        for (EffectDef def : definitions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", def.id());
            obj.addProperty("effect", def.effect());
            obj.addProperty("nutrient", def.nutrient());
            obj.addProperty("trigger", def.trigger());
            obj.addProperty("threshold", def.threshold());
            obj.addProperty("amplifier", def.amplifier());
            obj.addProperty("duration_ticks", def.durationTicks());
            obj.addProperty("enabled", def.enabled());
            obj.addProperty("threshold_max", def.thresholdMax());
            obj.addProperty("ambient", def.ambient());
            obj.addProperty("show_particles", def.showParticles());
            arr.add(obj);
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static List<EffectDef> loadBundledDefaults() {
        List<EffectDef> defaults = new ArrayList<>();
        for (String stem : DEFAULT_EFFECT_RESOURCES) {
            String resourcePath = "/data/nourished/nourished/effects/" + stem + ".json";
            try (InputStream in = EffectRegistry.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                    if (obj == null) {
                        continue;
                    }
                    String nutrient = obj.has("nutrient_key") ? obj.get("nutrient_key").getAsString() : "all";
                    String thresholdType = obj.has("threshold_type") ? obj.get("threshold_type").getAsString() : "LOW";
                    String trigger = switch (thresholdType.toUpperCase(java.util.Locale.ROOT)) {
                        case "EXCESS" -> "above";
                        case "BONUS" -> "all_above";
                        default -> "below";
                    };
                    defaults.add(new EffectDef(
                            stem,
                            obj.get("effect_id").getAsString(),
                            nutrient,
                            trigger,
                            obj.has("threshold") ? obj.get("threshold").getAsDouble() : 0.25,
                            obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0,
                            obj.has("duration") ? obj.get("duration").getAsInt() : 140,
                            true,
                            1.0,
                            true,
                            false
                    ));
                }
            } catch (IOException ignored) {
                // Keep startup resilient.
            }
        }
        return defaults;
    }
}
