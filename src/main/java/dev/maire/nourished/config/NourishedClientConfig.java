package dev.maire.nourished.config;

import com.google.gson.JsonObject;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

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
    private final ModConfigSpec.BooleanValue hideZeroNutrients;
    private final ModConfigSpec.ConfigValue<List<? extends String>> dietBarOrder;

    private NourishedClientConfig(ModConfigSpec.Builder builder) {
        JsonObject defaults = ConfigDefaultsLoader.loadOrEmpty("/data/nourished/config/client_defaults.json");
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
        hideZeroNutrients = builder.define("hideZeroNutrients", ConfigDefaultsLoader.getBoolean(defaults, "hideZeroNutrients", true));
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
    }

    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        bindIfOurs(event.getConfig());
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

    public boolean hideZeroNutrients() {
        return hideZeroNutrients.get();
    }

    public void setHideZeroNutrients(boolean value) {
        hideZeroNutrients.set(value);
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
