package dev.maire.nourished.client;

import dev.maire.nourished.core.color.ColorRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared ARGB palette for diet nutrient keys. Uses registry index for dynamic lookup.
 */
public final class NutrientUiColors {

    /** Transient UI overrides (e.g. config preview / color pickers); never persisted to disk. */
    private static final Map<String, Integer> OVERRIDES = new ConcurrentHashMap<>();

    private static final int[] PALETTE = {
            0xFF55FF55,  // green
            0xFF4DD9D9,  // cyan
            0xFFFFD65C,  // gold
            0xFFA95FFF,  // purple
            0xFFFFFFFF,  // white
            0xFFFF9955,  // orange
            0xFF55AAFF   // blue
    };

    private NutrientUiColors() {}

    /**
     * Sets a transient ARGB override for a nutrient key, or clears it when {@code argb} is null.
     * Used by live config previews and future per-nutrient color customization.
     */
    public static void setOverride(String key, Integer argb) {
        if (argb == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, argb);
        }
    }

    /** Clears all transient color overrides. */
    public static void clearOverrides() {
        OVERRIDES.clear();
    }

    /** Default palette entry for a key (no {@link ColorRegistry} or transient override). */
    public static int paletteOnlyArgb(String key) {
        List<String> keys = NutrientRegistry.getKeys();
        int idx = keys.indexOf(key);
        if (idx < 0) {
            return PALETTE[0];
        }
        return PALETTE[idx % PALETTE.length];
    }

    /**
     * ARGB accent color: transient override (e.g. config picker while editing), then {@link ColorRegistry},
     * then the built-in palette.
     */
    public static int baseColorArgb(String key) {
        Integer override = OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        return ColorRegistry.getArgb(key).orElseGet(() -> paletteOnlyArgb(key));
    }
}
