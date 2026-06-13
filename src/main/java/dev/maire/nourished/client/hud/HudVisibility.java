package dev.maire.nourished.client.hud;

import dev.marie.MariesLib.client.MarieClientCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.MariesLib.tracking.TrackingData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class HudVisibility {

    private HudVisibility() {}

    static List<String> visibleKeys(TrackingData data, List<String> keys, NourishedClientConfig cc) {
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

    static boolean dimZeroRow(float truePct, NourishedClientConfig cc) {
        return cc.hudShowZeroBars() && truePct <= HudVisibilityRules.ZERO_EPSILON;
    }
}
