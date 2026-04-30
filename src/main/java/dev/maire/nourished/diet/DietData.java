package dev.maire.nourished.diet;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Tracks daily-style diet totals. Nutrient keys are driven by {@link NutrientRegistry#getKeys()}.
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
}
