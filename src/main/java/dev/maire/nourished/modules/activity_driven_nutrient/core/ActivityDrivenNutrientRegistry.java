package dev.maire.nourished.modules.activity_driven_nutrient.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.marie.framework.resources.api.MarieResourcesAPI;
import dev.marie.framework.util.MarieJsonUtils;
import dev.marie.framework.util.MarieResourceLoader;
import dev.marie.framework.util.MarieValidation;
import dev.maire.nourished.core.Nourished;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-backed replacement for the old {@code ModConfig.Type.SERVER} activity-driven-nutrient spec.
 * Lives at {@code config/nourished/modules/activity/activity_config.json}, loaded/saved/reloaded
 * following {@link dev.marie.framework.color.ColorRegistry}'s exact shape.
 *
 * <p>These static fields are the single source of truth: on a dedicated/integrated server they hold
 * the authoritative values read from disk, and on a pure client that never runs its own logical
 * server they're overwritten by {@link #applyFromSync(CompoundTag)} once the server's snapshot
 * arrives — the same "one shared copy" behavior NeoForge's own SERVER-config sync used to provide.
 * Game logic ({@code ActivityModuleDispatcher}, the module classes) only ever runs against a
 * {@code ServerPlayer}, so on a pure client these fields are read solely for Cloth Config display.
 */
public final class ActivityDrivenNutrientRegistry {

    public static final String SYNC_ID = "activity_driven_nutrient";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_RELATIVE_PATH = "modules/activity/activity_config.json";
    private static final String DATAPACK_RELATIVE_PATH = "config/modules/activity/activity_config.json";

    private static final Map<String, Integer> DEFAULT_COLORS = Map.of(
            "mining", 0xFF8B6F47,
            "combat", 0xFFCC3333,
            "sprint", 0xFF33CC66,
            "swim", 0xFF3399CC,
            "starvation", 0xFFCC8833
    );

    private static volatile boolean enabled = true;
    private static volatile boolean sprintEnabled = true;
    private static volatile boolean swimEnabled = true;
    private static volatile boolean miningEnabled = true;
    private static volatile boolean combatEnabled = true;
    private static volatile boolean starvationEnabled = true;

    private static volatile double miningCostPerBlock = 0.0005d;
    private static volatile double combatCostPerKill = 0.01d;
    private static volatile double sprintDecayBoost = 0.0004d;
    private static volatile double swimDecayBoost = 0.0004d;
    private static volatile double starvationPenalty = 0.02d;

    private static final Map<String, Integer> COLORS = new LinkedHashMap<>(DEFAULT_COLORS);

    private ActivityDrivenNutrientRegistry() {}

    // ── Getters / setters — same shape as the old ModConfigSpec-backed getters ─────────────────

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean sprintEnabled() {
        return sprintEnabled;
    }

    public static void setSprintEnabled(boolean value) {
        sprintEnabled = value;
    }

    public static boolean swimEnabled() {
        return swimEnabled;
    }

    public static void setSwimEnabled(boolean value) {
        swimEnabled = value;
    }

    public static boolean miningEnabled() {
        return miningEnabled;
    }

    public static void setMiningEnabled(boolean value) {
        miningEnabled = value;
    }

    public static boolean combatEnabled() {
        return combatEnabled;
    }

    public static void setCombatEnabled(boolean value) {
        combatEnabled = value;
    }

    public static boolean starvationEnabled() {
        return starvationEnabled;
    }

    public static void setStarvationEnabled(boolean value) {
        starvationEnabled = value;
    }

    public static double miningCostPerBlock() {
        return miningCostPerBlock;
    }

    public static void setMiningCostPerBlock(double value) {
        miningCostPerBlock = value;
    }

    public static double combatCostPerKill() {
        return combatCostPerKill;
    }

    public static void setCombatCostPerKill(double value) {
        combatCostPerKill = value;
    }

    public static double sprintDecayBoost() {
        return sprintDecayBoost;
    }

    public static void setSprintDecayBoost(double value) {
        sprintDecayBoost = value;
    }

    public static double swimDecayBoost() {
        return swimDecayBoost;
    }

    public static void setSwimDecayBoost(double value) {
        swimDecayBoost = value;
    }

    public static double starvationPenalty() {
        return starvationPenalty;
    }

    public static void setStarvationPenalty(double value) {
        starvationPenalty = value;
    }

    // ── Per-module colors ────────────────────────────────────────────────────────────────────

    /** Effective color for {@code moduleId} (override or built-in default), or empty if {@code moduleId} is unknown. */
    public static Optional<Integer> getColor(String moduleId) {
        Integer v = COLORS.get(moduleId);
        if (v != null) {
            return Optional.of(v);
        }
        Integer def = DEFAULT_COLORS.get(moduleId);
        return def == null ? Optional.empty() : Optional.of(def);
    }

    public static int getDefaultColor(String moduleId) {
        return DEFAULT_COLORS.getOrDefault(moduleId, 0xFFFFFFFF);
    }

    /** Puts or replaces a module's color (alpha forced to 0xFF). */
    public static void setColor(String moduleId, int argb) {
        int opaque = (argb & 0x00_FF_FF_FF) | 0xFF00_0000;
        COLORS.put(moduleId, opaque);
    }

    /** Resets a module's color back to its built-in default. */
    public static void resetColor(String moduleId) {
        Integer def = DEFAULT_COLORS.get(moduleId);
        if (def != null) {
            COLORS.put(moduleId, def);
        } else {
            COLORS.remove(moduleId);
        }
    }

    // ── Load / save / reload / datapack — mirrors ColorRegistry's shape ────────────────────────

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve(CONFIG_RELATIVE_PATH);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                writeCurrent(file);
                Nourished.LOGGER.info("[ActivityDrivenNutrientRegistry] Wrote default activity_config.json");
            } else {
                parseFile(file);
            }
            Nourished.LOGGER.info("[ActivityDrivenNutrientRegistry] Loaded activity_config.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[ActivityDrivenNutrientRegistry] Failed to load activity_config.json, using defaults", e);
            resetToDefaults();
        }

        try {
            writeReadmeIfAbsent(file.getParent().resolve("Read_Me"));
        } catch (IOException e) {
            Nourished.LOGGER.warn("[ActivityDrivenNutrientRegistry] Failed to write ACTIVITY_CONFIG_README.md", e);
        }
    }

    private static void writeReadmeIfAbsent(Path readmeDir) throws IOException {
        Path readme = readmeDir.resolve("ACTIVITY_CONFIG_README.md");
        if (Files.exists(readme)) {
            return;
        }
        Files.createDirectories(readmeDir);
        String resourcePath = "/data/" + Nourished.MODID + "/config/ACTIVITY_CONFIG_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                Nourished.LOGGER.warn("[ActivityDrivenNutrientRegistry] No bundled ACTIVITY_CONFIG_README.md, skipping write");
                return;
            }
            Files.copy(in, readme);
        }
    }

    public static void reload() {
        Nourished.LOGGER.info("[ActivityDrivenNutrientRegistry] Reloading activity_config.json");
        load();
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve(CONFIG_RELATIVE_PATH);
        try {
            Files.createDirectories(file.getParent());
            writeCurrent(file);
            Nourished.LOGGER.info("[ActivityDrivenNutrientRegistry] Saved activity_config.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[ActivityDrivenNutrientRegistry] Failed to save activity_config.json", e);
        }
    }

    /**
     * Datapack path {@code <modid>:config/modules/activity/activity_config.json}. If absent, falls
     * back to {@link #load()}.
     */
    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                DATAPACK_RELATIVE_PATH,
                ActivityDrivenNutrientRegistry::parseFromReader,
                ActivityDrivenNutrientRegistry::load,
                "[ActivityDrivenNutrientRegistry] Loaded from datapack override",
                "[ActivityDrivenNutrientRegistry] Failed to load datapack activity config, falling back to config folder",
                "[ActivityDrivenNutrientRegistry] Loaded from config folder"
        );
    }

    private static void resetToDefaults() {
        enabled = true;
        sprintEnabled = true;
        swimEnabled = true;
        miningEnabled = true;
        combatEnabled = true;
        starvationEnabled = true;
        miningCostPerBlock = 0.0005d;
        combatCostPerKill = 0.01d;
        sprintDecayBoost = 0.0004d;
        swimDecayBoost = 0.0004d;
        starvationPenalty = 0.02d;
        COLORS.clear();
        COLORS.putAll(DEFAULT_COLORS);
    }

    private static void parseFile(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void parseFromReader(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            Nourished.LOGGER.warn("[ActivityDrivenNutrientRegistry] activity_config.json was empty, using defaults");
            resetToDefaults();
            return;
        }
        enabled = MarieJsonUtils.getOptionalBoolean(root, "enabled", true);
        sprintEnabled = MarieJsonUtils.getOptionalBoolean(root, "sprintEnabled", true);
        swimEnabled = MarieJsonUtils.getOptionalBoolean(root, "swimEnabled", true);
        miningEnabled = MarieJsonUtils.getOptionalBoolean(root, "miningEnabled", true);
        combatEnabled = MarieJsonUtils.getOptionalBoolean(root, "combatEnabled", true);
        starvationEnabled = MarieJsonUtils.getOptionalBoolean(root, "starvationEnabled", true);
        miningCostPerBlock = MarieJsonUtils.getOptionalDouble(root, "miningCostPerBlock", 0.0005d);
        combatCostPerKill = MarieJsonUtils.getOptionalDouble(root, "combatCostPerKill", 0.01d);
        sprintDecayBoost = MarieJsonUtils.getOptionalDouble(root, "sprintDecayBoost", 0.0004d);
        swimDecayBoost = MarieJsonUtils.getOptionalDouble(root, "swimDecayBoost", 0.0004d);
        starvationPenalty = MarieJsonUtils.getOptionalDouble(root, "starvationPenalty", 0.02d);

        COLORS.clear();
        COLORS.putAll(DEFAULT_COLORS);
        if (root.has("colors") && root.get("colors").isJsonObject()) {
            JsonObject colors = root.getAsJsonObject("colors");
            for (String moduleId : DEFAULT_COLORS.keySet()) {
                if (colors.has(moduleId)) {
                    COLORS.put(moduleId, parseArgbString(colors.get(moduleId).getAsString(), DEFAULT_COLORS.get(moduleId)));
                }
            }
        }
    }

    private static int parseArgbString(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return (int) (Long.parseLong(s.substring(2), 16) & 0xFF_FF_FF_FF) | 0xFF00_0000;
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
        return fallback;
    }

    private static void writeCurrent(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("sprintEnabled", sprintEnabled);
        root.addProperty("swimEnabled", swimEnabled);
        root.addProperty("miningEnabled", miningEnabled);
        root.addProperty("combatEnabled", combatEnabled);
        root.addProperty("starvationEnabled", starvationEnabled);
        root.addProperty("miningCostPerBlock", miningCostPerBlock);
        root.addProperty("combatCostPerKill", combatCostPerKill);
        root.addProperty("sprintDecayBoost", sprintDecayBoost);
        root.addProperty("swimDecayBoost", swimDecayBoost);
        root.addProperty("starvationPenalty", starvationPenalty);

        JsonObject colors = new JsonObject();
        for (Map.Entry<String, Integer> e : COLORS.entrySet()) {
            colors.addProperty(e.getKey(), String.format("0x%08X", e.getValue()));
        }
        root.add("colors", colors);

        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        MarieValidation.assertPathUnder(file, configDir, "ActivityDrivenNutrientRegistry");
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(root, w);
        }
    }

    // ── MarieResourcesAPI sync wiring ───────────────────────────────────────────────────────────

    /** Registers server→client sync hooks. Safe to call on any dist; must run during mod init. */
    public static void registerSync() {
        MarieResourcesAPI.registerConfigSyncSupplier(SYNC_ID, ActivityDrivenNutrientRegistry::toSnapshot);
        MarieResourcesAPI.registerConfigSyncClientHandler(SYNC_ID, ActivityDrivenNutrientRegistry::applyFromSync);
    }

    private static CompoundTag toSnapshot() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("sprintEnabled", sprintEnabled);
        tag.putBoolean("swimEnabled", swimEnabled);
        tag.putBoolean("miningEnabled", miningEnabled);
        tag.putBoolean("combatEnabled", combatEnabled);
        tag.putBoolean("starvationEnabled", starvationEnabled);
        tag.putDouble("miningCostPerBlock", miningCostPerBlock);
        tag.putDouble("combatCostPerKill", combatCostPerKill);
        tag.putDouble("sprintDecayBoost", sprintDecayBoost);
        tag.putDouble("swimDecayBoost", swimDecayBoost);
        tag.putDouble("starvationPenalty", starvationPenalty);

        CompoundTag colorsTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : COLORS.entrySet()) {
            colorsTag.putInt(e.getKey(), e.getValue());
        }
        tag.put("colors", colorsTag);
        return tag;
    }

    /** Applies a server-broadcast snapshot. Client-only; used solely for Cloth Config display. */
    private static void applyFromSync(CompoundTag tag) {
        enabled = tag.getBoolean("enabled");
        sprintEnabled = tag.getBoolean("sprintEnabled");
        swimEnabled = tag.getBoolean("swimEnabled");
        miningEnabled = tag.getBoolean("miningEnabled");
        combatEnabled = tag.getBoolean("combatEnabled");
        starvationEnabled = tag.getBoolean("starvationEnabled");
        miningCostPerBlock = tag.getDouble("miningCostPerBlock");
        combatCostPerKill = tag.getDouble("combatCostPerKill");
        sprintDecayBoost = tag.getDouble("sprintDecayBoost");
        swimDecayBoost = tag.getDouble("swimDecayBoost");
        starvationPenalty = tag.getDouble("starvationPenalty");

        if (tag.contains("colors")) {
            CompoundTag colorsTag = tag.getCompound("colors");
            for (String moduleId : DEFAULT_COLORS.keySet()) {
                if (colorsTag.contains(moduleId)) {
                    COLORS.put(moduleId, colorsTag.getInt(moduleId));
                }
            }
        }
    }
}
