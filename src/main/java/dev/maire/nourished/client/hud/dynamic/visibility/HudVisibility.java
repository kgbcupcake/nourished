package dev.maire.nourished.client.hud.dynamic.visibility;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.tracking.TrackingData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HudVisibility {

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

        return HudVisibilityRules.filter(
                data.values,
                keys,
                cc.hudShowZeroBars(),
                (float) cc.hudHideAboveThreshold(),
                (float) cc.hudShowAboveThreshold(),
                flashingKeys
        );
    }

    public static boolean dimZeroRow(float truePct, NourishedClientConfig cc) {
        return cc.hudShowZeroBars() && truePct <= HudVisibilityRules.ZERO_EPSILON;
    }
}
