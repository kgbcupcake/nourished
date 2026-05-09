package dev.maire.nourished.client;

import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.network.ModNetworking.SyncDietDeltaPayload;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientDietCache {

    public static final int FLASH_MS = 600;
    private static final float INCREASE_EPSILON = 0.005f;

    /**
     * Atomic client diet snapshot: {@link #diet} plus HUD lists that come from the server as a unit
     * (full sync derives lists from {@link DietData}; delta sync carries lists on the wire separately).
     * volatile: replaced whole on the network thread; read concurrently on the render thread.
     */
    private static volatile Snapshot current = Snapshot.initial();

    private record Snapshot(
            DietData diet,
            List<String> recentFoodIds,
            List<String> neglectedCategories,
            List<String> fatiguedFamilies
    ) {
        static Snapshot initial() {
            DietData d = new DietData();
            return new Snapshot(d, List.of(), List.of(), List.of());
        }

        static Snapshot fromFullDiet(DietData data) {
            DietData snapshot = DietData.copySnapshot(data);
            List<String> recent = snapshot.foodMemory.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().lastEatenTick(), a.getValue().lastEatenTick()))
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
            return new Snapshot(
                    snapshot,
                    recent,
                    snapshot.getMostNeglectedCategories(2),
                    snapshot.getMostFatiguedFamilies(2, snapshot.lastTickTime)
            );
        }

        static Snapshot fromDelta(DietData next, SyncDietDeltaPayload payload) {
            return new Snapshot(
                    next,
                    payload.recentFoodIds(),
                    payload.neglectedCategories(),
                    payload.fatiguedFamilies()
            );
        }
    }

    private static final Map<String, Long> lastNutrientIncreaseMs = new HashMap<>();
    /** Skip recording flashes on the first sync (login) so zeros-to-values does not flash every bar. */
    private static boolean firstClientSync = true;

    /**
     * Applies incoming full diet from the server: updates cache, records nutrient increases for bar flash
     * (except on the very first client sync this session). Used on login, respawn, dimension change.
     */
    public static void set(DietData data) {
        Snapshot prev = current;
        Snapshot next = Snapshot.fromFullDiet(data);
        if (!firstClientSync) {
            recordNutrientIncreases(prev.diet.nutrients, next.diet.nutrients);
        }
        firstClientSync = false;
        current = next;
    }

    /**
     * Apply a lightweight delta update — display fields only.
     * Does NOT touch foodMemory, categoryMemory, familyMemory — these are server-side only
     * and never exist on the client. The client DietData's memory maps are always empty.
     */
    public static void applyDelta(SyncDietDeltaPayload payload) {
        Snapshot prev = current;
        if (!firstClientSync) {
            recordNutrientIncreases(prev.diet.nutrients, payload.nutrients());
        }
        firstClientSync = false;

        DietData nextDiet = DietData.copySnapshot(prev.diet);
        nextDiet.nutrients.clear();
        nextDiet.nutrients.putAll(payload.nutrients());
        nextDiet.lastNutrients.clear();
        nextDiet.lastNutrients.putAll(payload.lastNutrients());
        nextDiet.calories = payload.calories();
        nextDiet.maxCalories = payload.maxCalories();
        current = Snapshot.fromDelta(nextDiet, payload);
    }

    private static void recordNutrientIncreases(Map<String, Float> prev, Map<String, Float> next) {
        List<String> keys = DietData.barOrder();
        long now = System.currentTimeMillis();
        for (String key : keys) {
            float before = prev.getOrDefault(key, 0f);
            float after = next.getOrDefault(key, 0f);
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
        Snapshot snap = current;
        return snap.diet;
    }

    public static float getBalanceScore() {
        Snapshot snap = current;
        return snap.diet.getBalanceScore();
    }

    public static List<String> getRecentFoodIds() {
        Snapshot snap = current;
        return snap.recentFoodIds;
    }

    public static List<String> getNeglectedCategories() {
        Snapshot snap = current;
        return snap.neglectedCategories;
    }

    public static List<String> getFatiguedFamilies() {
        Snapshot snap = current;
        return snap.fatiguedFamilies;
    }
}
