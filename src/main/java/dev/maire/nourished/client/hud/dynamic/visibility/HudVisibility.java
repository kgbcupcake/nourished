package dev.maire.nourished.client.hud.dynamic.visibility;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.core.Nourished;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HudVisibility {

    /**
     * Diagnostic-log throttle for {@link #logIfMasked}: at most once every 5s, so an always-on
     * "reveal on gain" bypass (or a config-read mismatch) doesn't spam the log every frame while
     * still surfacing within a few seconds of it happening.
     */
    private static final long DIAGNOSTIC_LOG_INTERVAL_MS = 5000L;
    private static long lastDiagnosticLogMs = 0L;

    private HudVisibility() {}

    public static List<String> visibleKeys(TrackingData data, List<String> keys, NourishedClientConfig cc) {
        Set<String> flashingKeys;
        if (cc.hudRevealOnNutrientGain()) {
            flashingKeys = new HashSet<>();
            for (String key : keys) {
                if (MarieClientCache.flashAlpha(key) > 0f) {
                    flashingKeys.add(key);
                }
            }
        } else {
            flashingKeys = Set.of();
        }

        float hideAbove = (float) cc.hudHideAboveThreshold();
        float showAbove = (float) cc.hudShowAboveThreshold();
        List<String> result = HudVisibilityRules.filter(
                data.values,
                keys,
                cc.hudShowZeroBars(),
                hideAbove,
                showAbove,
                flashingKeys
        );
        logIfMasked(data, keys, hideAbove, showAbove, flashingKeys, result);
        return result;
    }

    /**
     * Temporary diagnostic: the hide-above/show-above thresholds were reported broken in normal
     * play (not just edit mode), but the pure filter math and the config wiring both check out
     * under static review. Logs the exact runtime values whenever hide-above is active (< 1.0) yet
     * every row it should have hidden is still visible — either because {@code flashingKeys} is
     * covering every key (a "reveal on gain" bypass stuck on) or {@code data.values} disagrees with
     * what the threshold sliders show. Safe to remove once the real cause is confirmed from a log.
     */
    private static void logIfMasked(
            TrackingData data, List<String> keys, float hideAbove, float showAbove, Set<String> flashingKeys, List<String> result) {
        if (hideAbove >= 1.0f - HudVisibilityRules.ZERO_EPSILON) {
            return;
        }
        boolean anyShouldBeHidden = false;
        for (String key : keys) {
            float value = data.values.getOrDefault(key, 0f);
            if (value >= hideAbove && value < showAbove) {
                anyShouldBeHidden = true;
                break;
            }
        }
        if (!anyShouldBeHidden || result.size() < keys.size()) {
            // Either nothing should have been hidden right now, or at least one row correctly was
            // — the threshold is doing something, so this isn't the "always shown" symptom.
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDiagnosticLogMs < DIAGNOSTIC_LOG_INTERVAL_MS) {
            return;
        }
        lastDiagnosticLogMs = now;
        Nourished.LOGGER.info(
                "[Nourished] HUD threshold diagnostic: hideAbove={} showAbove={} flashingKeys={} values={} visible={} (expected at least one row hidden but none were)",
                hideAbove, showAbove, flashingKeys, data.values, result);
    }

    public static boolean dimZeroRow(float truePct, NourishedClientConfig cc) {
        return cc.hudShowZeroBars() && truePct <= HudVisibilityRules.ZERO_EPSILON;
    }
}
