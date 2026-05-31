package dev.maire.nourished.config;

import com.google.gson.JsonObject;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Client-only settings (HUD layout, diet screen UI). Stored in {@code nourished-client.toml}.
 */
public final class NourishedClientConfig {

    private static NourishedClientConfig INSTANCE;
    private static ModConfigSpec SPEC;
    private static volatile ModConfig boundClientConfig;

    private final ModConfigSpec.EnumValue<HudAnchor> hudAnchor;
    private final ModConfigSpec.IntValue hudOffsetX;
    private final ModConfigSpec.IntValue hudOffsetY;
    // private final ModConfigSpec.IntValue staminaHudOffsetX; // STAMINA_SHELVED
    // private final ModConfigSpec.IntValue staminaHudOffsetY; // STAMINA_SHELVED
    private final ModConfigSpec.IntValue hudBarWidth;
    private final ModConfigSpec.DoubleValue hudScale;
    private final ModConfigSpec.IntValue hudReservedBottom;
    private final ModConfigSpec.BooleanValue hudDraggable;
    private final ModConfigSpec.BooleanValue dietBarDragEnabled;
    private final ModConfigSpec.DoubleValue hudHideAboveThreshold;
    private final ModConfigSpec.DoubleValue hudShowAboveThreshold;
    private final ModConfigSpec.BooleanValue hudShowZeroBars;
    private final ModConfigSpec.DoubleValue hudBackgroundOpacity;
    private final ModConfigSpec.BooleanValue hudVerticalLayout;
    private final ModConfigSpec.ConfigValue<List<? extends String>> dietBarOrder;

    /** Matches legacy {@code COL_PANEL_BG} alpha ({@code 0xCC}). */
    private static final double DEFAULT_HUD_BACKGROUND_OPACITY = 204.0d / 255.0d;

    private NourishedClientConfig(ModConfigSpec.Builder builder) {
        JsonObject defaults = ConfigDefaultsLoader.loadOrEmpty("/data/" + Nourished.MODID + "/config/client_defaults.json");
        builder.push("gui");
        hudAnchor = builder.defineEnum(
                "hudAnchor",
                HudAnchor.BOTTOM_LEFT,
                Arrays.asList(HudAnchor.values())
        );
        hudOffsetX = builder.defineInRange("hudOffsetX", ConfigDefaultsLoader.getInt(defaults, "hudOffsetX", 0), -2000, 2000);
        hudOffsetY = builder.defineInRange("hudOffsetY", ConfigDefaultsLoader.getInt(defaults, "hudOffsetY", 0), -2000, 2000);
        // staminaHudOffsetX = builder.defineInRange("staminaHudOffsetX", ConfigDefaultsLoader.getInt(defaults, "staminaHudOffsetX", -10), -2000, 2000); // STAMINA_SHELVED
        // staminaHudOffsetY = builder.defineInRange("staminaHudOffsetY", ConfigDefaultsLoader.getInt(defaults, "staminaHudOffsetY", -60), -2000, 2000); // STAMINA_SHELVED
        hudBarWidth = builder.defineInRange("hudBarWidth", ConfigDefaultsLoader.getInt(defaults, "hudBarWidth", 60), 40, 120);
        hudScale = builder.defineInRange("hudScale", ConfigDefaultsLoader.getDouble(defaults, "hudScale", 1.0d), 0.5d, 1.5d);
        hudReservedBottom = builder.defineInRange("hudReservedBottom", ConfigDefaultsLoader.getInt(defaults, "hudReservedBottom", 52), 30, 100);
        hudDraggable = builder.define("hudDraggable", ConfigDefaultsLoader.getBoolean(defaults, "hudDraggable", true));
        dietBarDragEnabled = builder.define("dietBarDragEnabled", ConfigDefaultsLoader.getBoolean(defaults, "dietBarDragEnabled", true));
        hudHideAboveThreshold = builder.defineInRange(
                "hudHideAboveThreshold",
                ConfigDefaultsLoader.getDouble(defaults, "hudHideAboveThreshold", 1.0d),
                0.0d,
                1.0d
        );
        hudShowAboveThreshold = builder.defineInRange(
                "hudShowAboveThreshold",
                ConfigDefaultsLoader.getDouble(defaults, "hudShowAboveThreshold", 0.0d),
                0.0d,
                1.0d
        );
        hudShowZeroBars = builder.define(
                "hudShowZeroBars",
                ConfigDefaultsLoader.getBoolean(defaults, "hudShowZeroBars", false)
        );
        hudBackgroundOpacity = builder.defineInRange(
                "hudBackgroundOpacity",
                ConfigDefaultsLoader.getDouble(defaults, "hudBackgroundOpacity", DEFAULT_HUD_BACKGROUND_OPACITY),
                0.0d,
                1.0d
        );
        hudVerticalLayout = builder.define(
                "hudVerticalLayout",
                ConfigDefaultsLoader.getBoolean(defaults, "hudVerticalLayout", false)
        );
        dietBarOrder = builder.defineListAllowEmpty(
                "dietBarOrder",
                List::of,
                () -> "",
                o -> {
                    if (!(o instanceof String s)) {
                        return false;
                    }
                    try {
                        NourishedRegistryUtils.requireNutrientKey(s, "NourishedClientConfig.dietBarOrder");
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
        );
        builder.pop();
    }

    public static void register(ModContainer modContainer) {
        if (INSTANCE != null) {
            return;
        }
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new NourishedClientConfig(builder);
        SPEC = builder.build();
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC, Nourished.MODID + "-client.toml");
    }

    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        bindIfOurs(event.getConfig());
        migrateLegacyClientToml();
    }

    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        bindIfOurs(event.getConfig());
    }

    /**
     * Removes obsolete keys from older dev builds so NeoForge client config stays valid.
     */
    private static void migrateLegacyClientToml() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID + "-client.toml");
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            boolean changed = false;
            List<String> kept = new ArrayList<>(lines.size());
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("hideZeroNutrients")
                        || trimmed.startsWith("hudShowBelowThreshold")) {
                    changed = true;
                    continue;
                }
                kept.add(line);
            }
            if (changed) {
                Files.write(path, kept, StandardCharsets.UTF_8);
                Nourished.LOGGER.info("[Nourished] Removed obsolete client config keys from {}", path);
            }
        } catch (IOException ex) {
            Nourished.LOGGER.warn("[Nourished] Failed to migrate legacy client config at {}", path, ex);
        }
    }

    private static void bindIfOurs(net.neoforged.fml.config.ModConfig config) {
        if (!Nourished.MODID.equals(config.getModId()) || config.getType() != ModConfig.Type.CLIENT) {
            return;
        }
        if (config.getSpec() != SPEC) {
            return;
        }
        boundClientConfig = config;
    }

    public static NourishedClientConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NourishedClientConfig has not been registered yet.");
        }
        return INSTANCE;
    }

    public static ModConfigSpec spec() {
        return SPEC;
    }

    public static void saveNow() {
        var cfg = boundClientConfig;
        if (cfg == null) {
            return;
        }
        var loaded = cfg.getLoadedConfig();
        if (loaded != null) {
            loaded.save();
        }
    }

    public HudAnchor hudAnchor() {
        return hudAnchor.get();
    }

    public void setHudAnchor(HudAnchor value) {
        hudAnchor.set(value);
    }

    public int hudOffsetX() {
        return hudOffsetX.get();
    }

    public void setHudOffsetX(int value) {
        hudOffsetX.set(value);
    }

    public int hudOffsetY() {
        return hudOffsetY.get();
    }

    public void setHudOffsetY(int value) {
        hudOffsetY.set(value);
    }

    // public int staminaHudOffsetX() { // STAMINA_SHELVED
    //     return staminaHudOffsetX.get(); // STAMINA_SHELVED
    // } // STAMINA_SHELVED

    public int staminaHudOffsetX() {
        return -10;
    }

    // public void setStaminaHudOffsetX(int value) { // STAMINA_SHELVED
    //     staminaHudOffsetX.set(value); // STAMINA_SHELVED
    // } // STAMINA_SHELVED

    public void setStaminaHudOffsetX(int value) {
    }

    // public int staminaHudOffsetY() { // STAMINA_SHELVED
    //     return staminaHudOffsetY.get(); // STAMINA_SHELVED
    // } // STAMINA_SHELVED

    public int staminaHudOffsetY() {
        return -60;
    }

    // public void setStaminaHudOffsetY(int value) { // STAMINA_SHELVED
    //     staminaHudOffsetY.set(value); // STAMINA_SHELVED
    // } // STAMINA_SHELVED

    public void setStaminaHudOffsetY(int value) {
    }

    public int hudBarWidth() {
        return hudBarWidth.get();
    }

    public void setHudBarWidth(int value) {
        hudBarWidth.set(value);
    }

    public double hudScale() {
        return hudScale.get();
    }

    public void setHudScale(double value) {
        hudScale.set(value);
    }

    public int hudReservedBottom() {
        return hudReservedBottom.get();
    }

    public void setHudReservedBottom(int value) {
        hudReservedBottom.set(value);
    }

    public boolean hudDraggable() {
        return hudDraggable.get();
    }

    public void setHudDraggable(boolean value) {
        hudDraggable.set(value);
    }

    public boolean dietBarDragEnabled() {
        return dietBarDragEnabled.get();
    }

    public void setDietBarDragEnabled(boolean value) {
        dietBarDragEnabled.set(value);
    }

    public double hudHideAboveThreshold() {
        return hudHideAboveThreshold.get();
    }

    public void setHudHideAboveThreshold(double value) {
        hudHideAboveThreshold.set(Math.max(0.0d, Math.min(1.0d, value)));
    }

    public double hudShowAboveThreshold() {
        return hudShowAboveThreshold.get();
    }

    public void setHudShowAboveThreshold(double value) {
        hudShowAboveThreshold.set(Math.max(0.0d, Math.min(1.0d, value)));
    }

    public boolean hudShowZeroBars() {
        return hudShowZeroBars.get();
    }

    public void setHudShowZeroBars(boolean value) {
        hudShowZeroBars.set(value);
    }

    public double hudBackgroundOpacity() {
        return hudBackgroundOpacity.get();
    }

    public void setHudBackgroundOpacity(double value) {
        hudBackgroundOpacity.set(value);
    }

    public boolean hudVerticalLayout() {
        return hudVerticalLayout.get();
    }

    public void setHudVerticalLayout(boolean value) {
        hudVerticalLayout.set(value);
    }

    /**
     * Registry order, or saved order from config with any new nutrients appended.
     */
    public List<String> effectiveDietBarOrder() {
        List<String> reg = NutrientRegistry.getKeys();
        List<? extends String> saved = dietBarOrder.get();
        if (saved == null || saved.isEmpty()) {
            return new ArrayList<>(reg);
        }
        List<String> ordered = new ArrayList<>();
        for (String s : saved) {
            if (reg.contains(s) && !ordered.contains(s)) {
                ordered.add(s);
            }
        }
        for (String k : reg) {
            if (!ordered.contains(k)) {
                ordered.add(k);
            }
        }
        return ordered;
    }

    public void setDietBarOrder(List<String> keys) {
        List<String> copy = new ArrayList<>(Objects.requireNonNull(keys));
        dietBarOrder.set(copy);
    }

    public void resetHudOffsets() {
        hudOffsetX.set(0);
        hudOffsetY.set(0);
    }

    public void resetDietBarOrder() {
        dietBarOrder.set(List.of());
    }
}
