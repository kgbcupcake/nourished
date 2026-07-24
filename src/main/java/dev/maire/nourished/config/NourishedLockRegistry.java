package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.maire.nourished.core.Nourished;
import dev.marie.framework.data.DatapackSchema;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieResourceLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Loads Nourished config locks from {@code config/nourished/locks.json}.
 */
public final class NourishedLockRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class LockedCore extends AbstractRegistry<String, Boolean> {
        LockedCore() {
            super("NourishedLockRegistry.locked");
        }
    }

    private static final class ServerOnlyCore extends AbstractRegistry<String, Boolean> {
        ServerOnlyCore() {
            super("NourishedLockRegistry.serverOnly");
        }
    }

    private static final LockedCore LOCKED = new LockedCore();
    private static final ServerOnlyCore SERVER_ONLY = new ServerOnlyCore();

    private NourishedLockRegistry() {}

    public static boolean isLocked(String key) {
        return LOCKED.contains(key);
    }

    public static boolean isServerOnly(String key) {
        return SERVER_ONLY.contains(key);
    }

    public static Set<String> getLockedKeys() {
        return Set.copyOf(LOCKED.keys());
    }

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
                Nourished.LOGGER.info("[NourishedLockRegistry] Wrote default locks.json");
            }
            parse(file);
            Nourished.LOGGER.info("[NourishedLockRegistry] Loaded {} locked, {} server_only from config folder",
                    LOCKED.size(), SERVER_ONLY.size());
        } catch (IOException e) {
            Nourished.LOGGER.error("[NourishedLockRegistry] Failed to load locks.json", e);
            LOCKED.reset();
            SERVER_ONLY.reset();
            LOCKED.freeze();
            SERVER_ONLY.freeze();
        }

        try {
            Path readmeDir = configDir.resolve("Read_Me");
            Files.createDirectories(readmeDir);
            writeReadmeIfAbsent(readmeDir);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedLockRegistry] Failed to write LOCKS_README.md", e);
        }
    }

    private static void writeReadmeIfAbsent(Path readmeDir) throws IOException {
        Path readme = readmeDir.resolve("LOCKS_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + Nourished.MODID + "/config/LOCKS_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                Nourished.LOGGER.warn("[NourishedLockRegistry] No bundled LOCKS_README.md, skipping write");
                return;
            }
            Files.copy(in, readme);
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[NourishedLockRegistry] Reloading locks.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                DatapackSchema.CONFIG_LOCKS,
                NourishedLockRegistry::parseFromReader,
                NourishedLockRegistry::load,
                "[NourishedLockRegistry] Loaded from datapack override",
                "[NourishedLockRegistry] Failed to load from datapack, falling back to config folder",
                "[NourishedLockRegistry] Loaded from config folder"
        );
    }

    private static void parseFromReader(Reader reader) {
        LOCKED.reset();
        SERVER_ONLY.reset();

        JsonObject obj = GSON.fromJson(reader, JsonObject.class);
        if (obj != null) {
            if (obj.has("locked") && obj.get("locked").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("locked");
                int limit = 2000;
                if (arr.size() > limit) {
                    Nourished.LOGGER.warn("[NourishedLockRegistry] locked has {} entries, exceeding {} — truncating", arr.size(), limit);
                }
                for (int i = 0; i < Math.min(arr.size(), limit); i++) {
                    LOCKED.register(arr.get(i).getAsString(), Boolean.TRUE);
                }
            }

            if (obj.has("server_only") && obj.get("server_only").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("server_only");
                int limit = 2000;
                if (arr.size() > limit) {
                    Nourished.LOGGER.warn("[NourishedLockRegistry] server_only has {} entries, exceeding {} — truncating", arr.size(), limit);
                }
                for (int i = 0; i < Math.min(arr.size(), limit); i++) {
                    SERVER_ONLY.register(arr.get(i).getAsString(), Boolean.TRUE);
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
