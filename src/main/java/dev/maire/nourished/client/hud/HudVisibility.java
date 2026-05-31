package dev.maire.nourished.client.hud;

import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.diet.DietData;

import java.util.List;

final class HudVisibility {

    private HudVisibility() {}

    static List<String> visibleKeys(DietData data, List<String> keys, NourishedClientConfig cc) {
        return HudVisibilityRules.filter(
                data.nutrients,
                keys,
                cc.hudShowZeroBars(),
                (float) cc.hudHideAboveThreshold(),
                (float) cc.hudShowAboveThreshold()
        );
    }

    static boolean dimZeroRow(float truePct, NourishedClientConfig cc) {
        return cc.hudShowZeroBars() && truePct <= HudVisibilityRules.ZERO_EPSILON;
    }
}
