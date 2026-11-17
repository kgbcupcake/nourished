package dev.maire.nourished.core.deficiency;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.marie.framework.resources.api.MarieResourcesAPI;
import dev.marie.framework.util.MarieResourceLoader;
import dev.marie.framework.util.MarieValidation;
import dev.maire.nourished.config.NourishedConfigPaths;
import dev.maire.nourished.core.Nourished;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code config/nourished/gameplay/deficiency.json}, overridable by a datapack at
 * {@code <modid>:config/gameplay/deficiency.json}. The server's copy is synced to clients.
 */
public final class DeficiencyConfig {

    public static final String SYNC_ID = "deficiency";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATAPACK_RELATIVE_PATH = "config/gameplay/deficiency.json";
    private static final String README = "DEFICIENCY_README.md";

    private static volatile DeficiencySettings settings = DeficiencySettings.defaults();
    private static volatile JsonObject raw = DeficiencySettings.defaultJson();

    private DeficiencyConfig() {}

    public static DeficiencySettings get() {
        return settings;
    }

    public static Path file() {
        return NourishedConfigPaths.gameplay().resolve("deficiency.json");
    }

    public static void load() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                apply(DeficiencySettings.defaultJson());
                write(file);
                Nourished.LOGGER.info("[DeficiencyConfig] Wrote default deficiency.json");
            } else {
                try (Reader r = Files.newBufferedReader(file)) {
                    parse(r);
                }
            }
        } catch (Exception e) {
            Nourished.LOGGER.error("[DeficiencyConfig] Failed to load deficiency.json, using defaults", e);
            apply(DeficiencySettings.defaultJson());
        }
        writeReadmeIfAbsent();
    }

    public static void reload() {
        load();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            write(file());
        } catch (IOException e) {
            Nourished.LOGGER.error("[DeficiencyConfig] Failed to save deficiency.json", e);
        }
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                DATAPACK_RELATIVE_PATH,
                DeficiencyConfig::parse,
                DeficiencyConfig::load,
                "[DeficiencyConfig] Loaded from datapack override",
                "[DeficiencyConfig] Failed to load datapack deficiency config, falling back to config folder",
                "[DeficiencyConfig] Loaded from config folder"
        );
    }

    public static void registerSync() {
        MarieResourcesAPI.registerConfigSyncSupplier(SYNC_ID, DeficiencyConfig::toSnapshot);
        MarieResourcesAPI.registerConfigSyncClientHandler(SYNC_ID, DeficiencyConfig::applyFromSync);
    }

    private static void parse(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            Nourished.LOGGER.warn("[DeficiencyConfig] deficiency.json was empty, using defaults");
            root = DeficiencySettings.defaultJson();
        }
        apply(root);
    }

    private static void apply(JsonObject root) {
        settings = DeficiencySettings.fromJson(root);
        raw = root;
    }

    private static void write(Path file) throws IOException {
        MarieValidation.assertPathUnder(file, NourishedConfigPaths.root(), "DeficiencyConfig");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(raw, w);
        }
    }

    private static void writeReadmeIfAbsent() {
        Path readme = NourishedConfigPaths.readme().resolve(README);
        if (Files.exists(readme)) return;
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("data/" + Nourished.MODID + "/config/" + README)) {
            if (in == null) return;
            Files.createDirectories(readme.getParent());
            Files.copy(in, readme);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[DeficiencyConfig] Failed to write {}", README, e);
        }
    }

    private static CompoundTag toSnapshot() {
        CompoundTag tag = new CompoundTag();
        tag.putString("json", GSON.toJson(raw));
        return tag;
    }

    private static void applyFromSync(CompoundTag tag) {
        try {
            JsonObject root = GSON.fromJson(tag.getString("json"), JsonObject.class);
            if (root != null) apply(root);
        } catch (Exception e) {
            Nourished.LOGGER.warn("[DeficiencyConfig] Ignoring malformed synced config", e);
        }
    }
}
