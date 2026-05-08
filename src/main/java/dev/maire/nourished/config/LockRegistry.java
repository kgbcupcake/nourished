package dev.maire.nourished.config;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Loads config setting locks from config/nourished/locks.json.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>Empty defaults (no locks)</li>
 *   <li>config/nourished/locks.json</li>
 *   <li>data/nourished/config/locks.json (datapack override)</li>
 * </ol>
 * <p>
 * Locks control which config entries are visible and editable in the Cloth Config screen:
 * <ul>
 *   <li>{@code locked}: Keys hidden from the config screen entirely</li>
 *   <li>{@code server_only}: Keys visible but non-editable on multiplayer servers</li>
 * </ul>
 */
public class LockRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> LOCKED = new HashSet<>();
    private static final Set<String> SERVER_ONLY = new HashSet<>();

    /**
     * Returns true if the key is locked (should be hidden from config screen).
     */
    public static boolean isLocked(String key) {
        return LOCKED.contains(key);
    }

    /**
     * Returns true if the key is server-only (visible but non-editable in multiplayer).
     */
    public static boolean isServerOnly(String key) {
        return SERVER_ONLY.contains(key);
    }

    /** Returns all locked keys. */
    public static Set<String> getLockedKeys() {
        return Collections.unmodifiableSet(LOCKED);
    }

    /** Returns all server-only keys. */
    public static Set<String> getServerOnlyKeys() {
        return Collections.unmodifiableSet(SERVER_ONLY);
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("locks.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[LockRegistry] Wrote default locks.json");
            }
            parse(file);
            Nourished.LOGGER.info("[LockRegistry] Loaded {} locked, {} server_only from config folder",
                    LOCKED.size(), SERVER_ONLY.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[LockRegistry] Failed to load locks.json", e);
            LOCKED.clear();
            SERVER_ONLY.clear();
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[LockRegistry] Reloading locks.json");
        load();
    }

    /**
     * Attempts to load from datapack first, then falls back to config folder.
     * Call this from a reload listener when datapacks are available.
     */
    public static void loadFromDatapack(ResourceManager resourceManager) {
        ResourceLocation datapackPath = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "config/locks.json");
        Optional<Resource> resource = resourceManager.getResource(datapackPath);

        if (resource.isPresent()) {
            try (InputStream is = resource.get().open();
                 Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                parseFromReader(r);
                Nourished.LOGGER.info("[LockRegistry] Loaded from datapack override");
                return;
            } catch (IOException e) {
                Nourished.LOGGER.error("[LockRegistry] Failed to load from datapack, falling back to config folder", e);
            }
        }

        load();
        Nourished.LOGGER.info("[LockRegistry] Loaded from config folder");
    }

    private static void parseFromReader(Reader reader) {
        LOCKED.clear();
        SERVER_ONLY.clear();

        JsonObject obj = GSON.fromJson(reader, JsonObject.class);
        if (obj == null) {
            return;
        }

        if (obj.has("locked") && obj.get("locked").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("locked");
            for (JsonElement el : arr) {
                LOCKED.add(el.getAsString());
            }
        }

        if (obj.has("server_only") && obj.get("server_only").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("server_only");
            for (JsonElement el : arr) {
                SERVER_ONLY.add(el.getAsString());
            }
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonObject obj = new JsonObject();
        obj.add("locked", new JsonArray());
        obj.add("server_only", new JsonArray());
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(obj, w);
        }
    }
}
