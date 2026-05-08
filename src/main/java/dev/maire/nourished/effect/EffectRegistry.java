package dev.maire.nourished.effect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import java.util.List;
import java.util.Optional;

/**
 * Loads effect definitions from config/nourished/effects.json.
 * Writes a default file on first run. Call {@link #load()} after
 * {@link dev.maire.nourished.nutrition.NutrientRegistry#load()}.
 */
public class EffectRegistry {

    public record EffectDef(
            String id,
            String effect,
            String nutrient,
            String trigger,
            double threshold,
            int amplifier,
            int durationTicks,
            boolean enabled
    ) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<EffectDef> REGISTRY = new ArrayList<>();

    private static final List<EffectDef> DEFAULTS = List.of(
            new EffectDef("protein_penalty", "minecraft:mining_fatigue", "proteins", "below", 0.25, 0, 140, true),
            new EffectDef("carbs_penalty", "minecraft:weakness", "grains", "below", 0.25, 0, 140, true),
            new EffectDef("vitamins_penalty", "minecraft:unluck", "fruits", "below", 0.25, 0, 140, true),
            new EffectDef("hydration_penalty", "minecraft:slowness", "vegetables", "below", 0.25, 0, 140, true),
            new EffectDef("all_high_health", "minecraft:health_boost", "all", "all_above", 0.75, 0, 140, true),
            new EffectDef("all_high_regen", "minecraft:regeneration", "all", "all_above", 0.75, 0, 140, true)
    );

    public static List<EffectDef> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY));
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

    private static void parseFromReader(Reader reader) {
        REGISTRY.clear();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr == null || arr.isEmpty()) {
            Nourished.LOGGER.warn("[EffectRegistry] Data was empty, using built-in defaults");
            loadDefaults();
            return;
        }
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String effect = obj.get("effect").getAsString();
            String nutrient = obj.get("nutrient").getAsString();
            String trigger = obj.get("trigger").getAsString();
            double threshold = obj.has("threshold") ? obj.get("threshold").getAsDouble() : 0.25;
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            int durationTicks = obj.has("duration_ticks") ? obj.get("duration_ticks").getAsInt() : 140;
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            REGISTRY.add(new EffectDef(id, effect, nutrient, trigger, threshold, amplifier, durationTicks, enabled));
        }
        if (REGISTRY.isEmpty()) {
            loadDefaults();
        }
    }

    private static void parse(Path file) throws IOException {
        REGISTRY.clear();
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String effect = obj.get("effect").getAsString();
                String nutrient = obj.get("nutrient").getAsString();
                String trigger = obj.get("trigger").getAsString();
                double threshold = obj.has("threshold") ? obj.get("threshold").getAsDouble() : 0.25;
                int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
                int durationTicks = obj.has("duration_ticks") ? obj.get("duration_ticks").getAsInt() : 140;
                boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
                REGISTRY.add(new EffectDef(id, effect, nutrient, trigger, threshold, amplifier, durationTicks, enabled));
            }
        }
        if (REGISTRY.isEmpty()) {
            Nourished.LOGGER.warn("[EffectRegistry] effects.json was empty, using built-in defaults");
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        REGISTRY.clear();
        REGISTRY.addAll(DEFAULTS);
    }

    private static void writeDefaults(Path file) throws IOException {
        writeDefinitionsToPath(file, DEFAULTS);
    }

    /**
     * Writes the given definitions to {@code config/nourished/effects.json} and replaces the in-memory registry.
     */
    public static void saveAll(List<EffectDef> definitions) throws IOException {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("effects.json");
        Files.createDirectories(configDir);
        writeDefinitionsToPath(file, definitions);
        REGISTRY.clear();
        for (EffectDef def : definitions) {
            REGISTRY.add(def);
        }
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
            arr.add(obj);
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
