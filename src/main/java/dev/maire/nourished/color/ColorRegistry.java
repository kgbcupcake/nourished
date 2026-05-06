package dev.maire.nourished.color;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-nutrient HUD/UI colors from {@code config/nourished/colors.json}, with optional datapack override
 * {@code nourished:config/colors.json} ({@code data/nourished/config/colors.json}).
 * <p>File format (JSON array):
 * <pre>{@code
 * [
 *   {"key": "fruits", "argb": "0xFF55FF55"},
 *   {"key": "proteins", "argb": "0xFF4DD9D9"}
 * ]
 * }</pre>
 * Call {@link #load()} after {@link dev.maire.nourished.effect.EffectRegistry#load()}.
 */
public final class ColorRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ConcurrentHashMap<String, Integer> COLORS = new ConcurrentHashMap<>();

    private ColorRegistry() {}

    public static Optional<Integer> getArgb(String key) {
        Integer v = COLORS.get(key);
        return v == null ? Optional.empty() : Optional.of(v);
    }

    /** Puts or replaces a color (alpha forced to 0xFF). */
    public static void setArgb(String key, int argb) {
        int opaque = (argb & 0x00_FF_FF_FF) | 0xFF00_0000;
        COLORS.put(key, opaque);
    }

    /** Removes a custom color so UI falls back to the default palette. */
    public static void remove(String key) {
        COLORS.remove(key);
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("colors.json");
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeEmpty(file);
                Nourished.LOGGER.info("[ColorRegistry] Wrote default colors.json");
            }
            parseFile(file);
            Nourished.LOGGER.info("[ColorRegistry] Loaded {} custom colors from {}", COLORS.size(), file);
        } catch (IOException e) {
            Nourished.LOGGER.error("[ColorRegistry] Failed to load colors.json", e);
            COLORS.clear();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[ColorRegistry] Reloading colors.json");
        load();
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("colors.json");
        try {
            Files.createDirectories(configDir);
            JsonArray arr = new JsonArray();
            List<String> keys = NutrientRegistry.getKeys();
            for (String key : keys) {
                Integer argb = COLORS.get(key);
                if (argb == null) {
                    continue;
                }
                JsonObject obj = new JsonObject();
                obj.addProperty("key", key);
                obj.addProperty("argb", String.format("0x%08X", argb));
                arr.add(obj);
            }
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(arr, w);
            }
            Nourished.LOGGER.info("[ColorRegistry] Saved {} entries to {}", arr.size(), file);
        } catch (IOException e) {
            Nourished.LOGGER.error("[ColorRegistry] Failed to save colors.json", e);
        }
    }

    /**
     * Datapack path {@code nourished:config/colors.json} ({@code data/nourished/config/colors.json}).
     * If absent, falls back to {@link #load()}.
     */
    public static void loadFromDatapack(ResourceManager resourceManager) {
        ResourceLocation datapackPath = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "config/colors.json");
        Optional<Resource> resource = resourceManager.getResource(datapackPath);
        if (resource.isPresent()) {
            try (InputStream is = resource.get().open();
                 Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                parseFromReader(r);
                Nourished.LOGGER.info("[ColorRegistry] Loaded from datapack override");
                return;
            } catch (IOException e) {
                Nourished.LOGGER.error("[ColorRegistry] Failed to load datapack colors, falling back to config folder", e);
            }
        }
        load();
        Nourished.LOGGER.info("[ColorRegistry] Loaded from config folder");
    }

    private static void writeEmpty(Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(new JsonArray(), w);
        }
    }

    private static void parseFile(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void parseFromReader(Reader reader) {
        COLORS.clear();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr == null || arr.isEmpty()) {
            return;
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("key")) {
                continue;
            }
            String key = obj.get("key").getAsString();
            int argb = parseArgbElement(obj.get("argb"));
            setArgb(key, argb);
        }
    }

    private static int parseArgbElement(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return 0xFF_FF_FF_FF;
        }
        if (el.isJsonPrimitive()) {
            if (el.getAsJsonPrimitive().isNumber()) {
                int n = el.getAsInt();
                return n | 0xFF00_0000;
            }
            return parseArgbString(el.getAsString());
        }
        return 0xFF_FF_FF_FF;
    }

    private static int parseArgbString(String raw) {
        if (raw == null) {
            return 0xFF_FF_FF_FF;
        }
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return Integer.decode(s) | 0xFF00_0000;
            }
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            if (s.length() == 6) {
                return (int) (Long.parseLong(s, 16) & 0xFF_FF_FF) | 0xFF00_0000;
            }
            if (s.length() == 8) {
                return (int) (Long.parseLong(s, 16) & 0xFF_FF_FF_FF);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0xFF_FF_FF_FF;
    }

}
