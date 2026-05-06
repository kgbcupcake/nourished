package dev.maire.nourished.diet;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks daily-style diet totals. Nutrient keys are driven by {@link NutrientRegistry#getKeys()}.
 * <p>
 * SCHEMA CHANGE: NutritionAttachment removed in this version. Existing player saves will reset diet data.
 * Acceptable for alpha.
 */
public class DietData {

    /** Display / bar order — delegates to the registry so it stays in sync. */
    public static List<String> barOrder() {
        return NutrientRegistry.getKeys();
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final Decoder<DietData> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<DietData, T>> decode(DynamicOps<T> ops, T input) {
            return decodeData(ops, input).map(d -> Pair.of(d, input));
        }
    };

    public static final Codec<DietData> CODEC = Codec.of(DietData::encode, DECODER);

    private static <T> DataResult<T> encode(DietData data, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> map = ops.mapBuilder();
        map.add("calories",     Codec.FLOAT.encodeStart(ops, data.calories));
        map.add("max_calories", Codec.FLOAT.encodeStart(ops, data.maxCalories));
        for (String key : barOrder()) {
            map.add(key,           Codec.FLOAT.encodeStart(ops, data.nutrients.getOrDefault(key, 0f)));
            map.add("last_" + key, Codec.FLOAT.encodeStart(ops, data.lastNutrients.getOrDefault(key, 0f)));
        }
        map.add("food_memory", Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                .encodeStart(ops, data.foodMemory));
        return map.build(prefix);
    }

    private static <T> DataResult<DietData> decodeData(DynamicOps<T> ops, T input) {
        MapLike<T> map = ops.getMap(input).getOrThrow();

        DietData data = new DietData();
        data.calories    = decodeFloat(ops, map, "calories",     0f);
        data.maxCalories = decodeFloat(ops, map, "max_calories", 2000f);

        for (String key : barOrder()) {
            data.nutrients.put(key,     decodeFloat(ops, map, key,           0f));
            data.lastNutrients.put(key, decodeFloat(ops, map, "last_" + key, 0f));
        }

        T foodMemoryVal = map.get("food_memory");
        if (foodMemoryVal != null) {
            Codec.unboundedMap(Codec.STRING, FoodMemoryEntry.CODEC)
                    .parse(ops, foodMemoryVal)
                    .result()
                    .ifPresent(m -> data.foodMemory.putAll(m));
        }

        return DataResult.success(data);
    }

    private static <T> float decodeFloat(DynamicOps<T> ops, MapLike<T> map, String field, float fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.FLOAT.parse(ops, val).result().orElse(fallback);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    public float calories;
    public float maxCalories = 2000f;
    public final LinkedHashMap<String, Float> nutrients     = new LinkedHashMap<>();
    public final LinkedHashMap<String, Float> lastNutrients = new LinkedHashMap<>();
    public final LinkedHashMap<String, FoodMemoryEntry> foodMemory = new LinkedHashMap<>();

    public DietData() {
        for (String key : barOrder()) {
            nutrients.put(key, 0f);
            lastNutrients.put(key, 0f);
        }
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /**
     * Snapshot current nutrients into {@link #lastNutrients}. Call on the server before applying
     * food deltas so trends compare the previous meal state to the new state.
     */
    public void tick() {
        lastNutrients.clear();
        lastNutrients.putAll(nutrients);
    }

    public void addCalories(float amount) {
        calories = Math.max(0f, calories + amount);
    }

    public void addNutrient(String key, float amount) {
        if (!nutrients.containsKey(key)) return;
        nutrients.put(key, Mth.clamp(nutrients.get(key) + amount, 0f, 1f));
    }

    /** Balance score in [0, 1]: higher when all bars are closer to each other. */
    public float getBalanceScore() {
        List<String> keys = barOrder();
        float sum = 0f;
        for (String k : keys) sum += nutrients.getOrDefault(k, 0f);
        float avg = sum / keys.size();
        if (avg <= 0.0001f) return 1f;
        float dev = 0f;
        for (String k : keys) dev += Math.abs(nutrients.getOrDefault(k, 0f) - avg);
        return 1f - Math.min(1f, dev / (avg * keys.size() * 2f));
    }

    // ── Food Memory ────────────────────────────────────────────────────────────

    /**
     * Purge expired entries, enforce count cap (evict oldest by lastEatenMs), then record this eat.
     * Returns the multiplier to apply to this food's DietDelta.
     */
    public float recordEat(String itemId) {
        long windowMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        int maxCount = NourishedConfig.get().memoryWindowCount();

        foodMemory.entrySet().removeIf(e -> e.getValue().isExpired(windowMs));

        if (!foodMemory.containsKey(itemId) && foodMemory.size() >= maxCount) {
            String oldest = foodMemory.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().lastEatenMs()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest != null) foodMemory.remove(oldest);
        }

        FoodMemoryEntry entry = foodMemory.getOrDefault(itemId, new FoodMemoryEntry(0, System.currentTimeMillis()));
        entry = entry.withEat();
        foodMemory.put(itemId, entry);
        return resolveMultiplier(entry.eatCount());
    }

    /**
     * Peek the multiplier that would apply if this food were eaten next.
     * Used for tooltip display.
     */
    public float peekMultiplier(String itemId) {
        long windowMs = NourishedConfig.get().memoryWindowMinutes() * 60_000L;
        FoodMemoryEntry entry = foodMemory.get(itemId);
        if (entry == null || entry.isExpired(windowMs)) return 1.0f;
        return resolveMultiplier(entry.eatCount() + 1);
    }

    private float resolveMultiplier(int eatCount) {
        var steps = NourishedConfig.get().diminishingSteps();
        double floor = NourishedConfig.get().diminishingFloor();
        int idx = Math.min(eatCount - 1, steps.size() - 1);
        double raw = idx >= 0 ? steps.get(idx) : 1.0;
        return (float) Math.max(floor, raw);
    }

    public FoodMemoryEntry getMemoryEntry(String itemId) {
        return foodMemory.get(itemId);
    }
}
