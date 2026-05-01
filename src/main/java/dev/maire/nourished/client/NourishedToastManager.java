package dev.maire.nourished.client;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietData;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Queues client toasts when diet sync shows a nutrient crossing into the critical band.
 */
public final class NourishedToastManager {

    private NourishedToastManager() {}

    /**
     * Call after client diet data was updated from the server. Detects threshold crossings
     * (was at or above critical, now strictly below) and enqueues one toast per nutrient.
     *
     * @param skipInitial when true, do nothing (used for the first diet sync after connect).
     */
    public static void onDietSynced(DietData prev, DietData next, boolean skipInitial) {
        if (skipInitial) {
            return;
        }
        NourishedConfig config = NourishedConfig.get();
        if (!config.enableEffects()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        List<String> keys = DietData.barOrder();
        for (String key : keys) {
            double crit = config.criticalThresholdFor(key);
            float before = prev.nutrients.getOrDefault(key, 0f);
            float after = next.nutrients.getOrDefault(key, 0f);
            if (before >= crit && after < crit) {
                mc.getToasts().addToast(new CriticalNutrientToast(key));
            }
        }
    }
}
