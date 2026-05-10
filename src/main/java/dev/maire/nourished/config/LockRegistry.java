package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.registry.AbstractRegistry;
import dev.maire.nourished.core.util.NourishedResourceLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final class LockedCore extends AbstractRegistry<String, Boolean> {
        LockedCore() {
            super("LockRegistry.locked");
        }
    }

    private static final class ServerOnlyCore extends AbstractRegistry<String, Boolean> {
        ServerOnlyCore() {
            super("LockRegistry.serverOnly");
        }
    }

    private static final LockedCore LOCKED = new LockedCore();
    private static final ServerOnlyCore SERVER_ONLY = new ServerOnlyCore();

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
        return Set.copyOf(LOCKED.keys());
    }

    /** Returns all server-only keys. */
    public static Set<String> getServerOnlyKeys() {
        return Set.copyOf(SERVER_ONLY.keys());
    }

    public static void replaceFromDatapack(Set<String> locked, Set<String> serverOnly) {
        LOCKED.reset();
        for (String k : locked) {
            LOCKED.register(k, Boolean.TRUE);
        }
        LOCKED.freeze();
        SERVER_ONLY.reset();
        for (String k : serverOnly) {
            SERVER_ONLY.register(k, Boolean.TRUE);
        }
        SERVER_ONLY.freeze();
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
            LOCKED.reset();
            SERVER_ONLY.reset();
            LOCKED.freeze();
            SERVER_ONLY.freeze();
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
        NourishedResourceLoader.loadFromModConfig(
                resourceManager,
                "config/locks.json",
                LockRegistry::parseFromReader,
                LockRegistry::load,
                "[LockRegistry] Loaded from datapack override",
                "[LockRegistry] Failed to load from datapack, falling back to config folder",
                "[LockRegistry] Loaded from config folder"
        );
    }

    private static void parseFromReader(Reader reader) {
        LOCKED.reset();
        SERVER_ONLY.reset();

        JsonObject obj = GSON.fromJson(reader, JsonObject.class);
        if (obj != null) {
            if (obj.has("locked") && obj.get("locked").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("locked");
                for (JsonElement el : arr) {
                    LOCKED.register(el.getAsString(), Boolean.TRUE);
                }
            }

            if (obj.has("server_only") && obj.get("server_only").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("server_only");
                for (JsonElement el : arr) {
                    SERVER_ONLY.register(el.getAsString(), Boolean.TRUE);
                }
            }
        }

        LOCKED.freeze();
        SERVER_ONLY.freeze();
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
