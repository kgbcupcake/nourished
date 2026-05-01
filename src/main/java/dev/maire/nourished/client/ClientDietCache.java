package dev.maire.nourished.client;

import dev.maire.nourished.diet.DietData;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientDietCache {

    public static final int FLASH_MS = 600;
    private static final float INCREASE_EPSILON = 0.005f;

    private static DietData current = new DietData();
    private static final Map<String, Long> lastNutrientIncreaseMs = new HashMap<>();
    /** Skip recording flashes on the first sync (login) so zeros-to-values does not flash every bar. */
    private static boolean firstClientSync = true;

    /**
     * Applies incoming diet from the server: updates cache, records nutrient increases for bar flash
     * (except on the very first client sync this session).
     */
    public static void set(DietData data) {
        DietData prev = current;
        boolean skipEdgeEffects = firstClientSync;
        if (!firstClientSync) {
            recordNutrientIncreases(prev, data);
        }
        firstClientSync = false;
        current = data;
        NourishedToastManager.onDietSynced(prev, data, skipEdgeEffects);
    }

    private static void recordNutrientIncreases(DietData prev, DietData next) {
        List<String> keys = DietData.barOrder();
        long now = System.currentTimeMillis();
        for (String key : keys) {
            float before = prev.nutrients.getOrDefault(key, 0f);
            float after = next.nutrients.getOrDefault(key, 0f);
            if (after > before + INCREASE_EPSILON) {
                lastNutrientIncreaseMs.put(key, now);
            }
        }
    }

    /**
     * Highlight strength in [0, 1]: full at flash start, zero after {@link #FLASH_MS}.
     */
    public static float flashAlpha(String key) {
        Long t = lastNutrientIncreaseMs.get(key);
        if (t == null) {
            return 0f;
        }
        long elapsed = System.currentTimeMillis() - t;
        if (elapsed >= FLASH_MS) {
            return 0f;
        }
        return Mth.clamp(1f - (float) elapsed / (float) FLASH_MS, 0f, 1f);
    }

    public static DietData get() {
        return current;
    }
}
