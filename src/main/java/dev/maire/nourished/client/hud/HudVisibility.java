package dev.maire.nourished.client.hud;

import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.diet.DietData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class HudVisibility {

    private HudVisibility() {}

    static List<String> visibleKeys(DietData data, List<String> keys, NourishedClientConfig cc) {
        Set<String> flashingKeys;
        if (cc.hudRevealOnNutrientGain()) {
            flashingKeys = new HashSet<>();
            for (String key : keys) {
                if (ClientDietCache.flashAlpha(key) > 0f) {
                    flashingKeys.add(key);
                }
            }
        } else {
            flashingKeys = Set.of();
        }

        return HudVisibilityRules.filter(
                data.nutrients,
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
