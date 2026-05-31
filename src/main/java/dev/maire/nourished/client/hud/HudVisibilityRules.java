package dev.maire.nourished.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure visibility rules for HUD nutrient bars. Extracted for unit testing without config wiring.
 */
public final class HudVisibilityRules {

    static final float ZERO_EPSILON = 1e-4f;

    private HudVisibilityRules() {}

    /**
     * @param showZero     when true, include 0% bars (dimmed in the renderer)
     * @param hideAbove    hide non-zero bars at/above this level; 1.0 disables
     * @param showAbove    re-enable non-zero bars at/above this level even when hide would apply; 1.0 disables; 0.0 re-enables all hidden bars
     */
    public static List<String> filter(
            Map<String, Float> nutrients,
            List<String> keys,
            boolean showZero,
            float hideAbove,
            float showAbove
    ) {
        hideAbove = Math.max(0f, Math.min(1f, hideAbove));
        showAbove = Math.max(0f, Math.min(1f, showAbove));
        boolean hideActive = hideAbove < 1f - ZERO_EPSILON;
        boolean showActive = showAbove < 1f - ZERO_EPSILON;

        if (showZero && !hideActive && !showActive) {
            return new ArrayList<>(keys);
        }

        List<String> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            float value = nutrients.getOrDefault(key, 0f);

            if (value <= ZERO_EPSILON) {
                if (showZero) {
                    result.add(key);
                }
                continue;
            }

            boolean visible = true;
            if (hideActive && value >= hideAbove) {
                visible = false;
            }
            if (showActive && value >= showAbove) {
                visible = true;
            }
            if (visible) {
                result.add(key);
            }
        }
        return result;
    }
}
