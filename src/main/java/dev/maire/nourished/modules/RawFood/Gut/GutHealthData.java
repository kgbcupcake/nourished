package dev.maire.nourished.modules.RawFood.Gut;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import net.minecraft.util.Mth;

/**
 * Tracks per-player gut health and sensitivity state.
 *
 * <p>Gut health degrades when eating raw food and recovers passively over time
 * or when eating cooked food. Sensitivity builds up from repeated raw eating
 * and makes penalties progressively worse.</p>
 */
@ApiStatus.Internal
public class GutHealthData {

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final Decoder<GutHealthData> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<GutHealthData, T>> decode(DynamicOps<T> ops, T input) {
            return decodeData(ops, input).map(d -> Pair.of(d, input));
        }
    };

    public static final Codec<GutHealthData> CODEC = Codec.of(GutHealthData::encode, DECODER);

    private static <T> DataResult<T> encode(GutHealthData data, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> map = ops.mapBuilder();
        map.add("gut_health", Codec.FLOAT.encodeStart(ops, data.gutHealth));
        map.add("sensitivity", Codec.FLOAT.encodeStart(ops, data.sensitivity));
        map.add("last_update_ms", Codec.LONG.encodeStart(ops, data.lastUpdateMs));
        return map.build(prefix);
    }

    private static <T> DataResult<GutHealthData> decodeData(DynamicOps<T> ops, T input) {
        MapLike<T> map = ops.getMap(input).getOrThrow();

        GutHealthData data = new GutHealthData();
        data.gutHealth = decodeFloat(ops, map, "gut_health", 1.0f);
        data.sensitivity = decodeFloat(ops, map, "sensitivity", 0.0f);
        data.lastUpdateMs = decodeLong(ops, map, "last_update_ms", 0L);

        return DataResult.success(data);
    }

    private static <T> float decodeFloat(DynamicOps<T> ops, MapLike<T> map, String field, float fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.FLOAT.parse(ops, val).result().orElse(fallback);
    }

    private static <T> long decodeLong(DynamicOps<T> ops, MapLike<T> map, String field, long fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.LONG.parse(ops, val).result().orElse(fallback);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private float gutHealth;
    private float sensitivity;
    private long lastUpdateMs;

    public GutHealthData() {
        this.gutHealth = 1.0f;
        this.sensitivity = 0.0f;
        this.lastUpdateMs = 0L;
    }

    /**
     * Creates a GutHealthData instance from sync payload values.
     * Used on the client to apply server state.
     */
    public static GutHealthData fromSync(float gutHealth, float sensitivity) {
        GutHealthData data = new GutHealthData();
        data.gutHealth = Mth.clamp(gutHealth, 0.0f, 1.0f);
        data.sensitivity = Mth.clamp(sensitivity, 0.0f, 1.0f);
        return data;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public float getGutHealth() {
        return gutHealth;
    }

    public float getSensitivity() {
        return sensitivity;
    }

    public long getLastUpdateMs() {
        return lastUpdateMs;
    }

    public void setLastUpdateMs(long ms) {
        this.lastUpdateMs = ms;
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /**
     * Applies gut damage from eating raw food.
     *
     * @param cookedness   how cooked the food is (0.0 = raw, 1.0 = fully cooked)
     * @param basePenalty  the base penalty for this tier of raw food
     */
    public void applyRawEat(float cookedness, float basePenalty) {
        float damage = basePenalty * (1.0f - cookedness) * (1.0f + sensitivity);
        gutHealth = Mth.clamp(gutHealth - damage, 0.0f, 1.0f);

        float sensitivityIncrement = RawFoodConfig.sensitivityIncrementPerRawEat();
        sensitivity = Mth.clamp(sensitivity + sensitivityIncrement, 0.0f, 1.0f);
    }

    /**
     * Applies passive or cooked-food recovery to gut health.
     *
     * @param amount the amount to recover
     */
    public void applyRecovery(float amount) {
        gutHealth = Mth.clamp(gutHealth + amount, 0.0f, 1.0f);
    }

    /**
     * Applies bonus recovery based on diet diversity (balance score).
     *
     * @param balanceScore  the player's current diet balance score
     * @param threshold     minimum balance score to trigger bonus
     * @param bonusRate     recovery rate per point of balance above threshold
     */
    public void applyDiversityBonus(float balanceScore, float threshold, float bonusRate) {
        if (balanceScore >= threshold) {
            float bonus = bonusRate * (balanceScore - threshold);
            gutHealth = Mth.clamp(gutHealth + bonus, 0.0f, 1.0f);
        }
    }

    /**
     * Decays sensitivity over time.
     *
     * @param decayRate the amount to decay per tick interval
     */
    public void applySensitivityDecay(float decayRate) {
        sensitivity = Mth.clamp(sensitivity - decayRate, 0.0f, 1.0f);
    }

    /**
     * Returns the effective penalty multiplier based on current sensitivity.
     * Used to scale raw food penalties.
     *
     * @return multiplier in range [1.0, 1.0 + maxSensitivityMultiplier]
     */
    public float effectivePenaltyMultiplier() {
        float maxMultiplier = RawFoodConfig.maxSensitivityMultiplier();
        return 1.0f + (sensitivity * maxMultiplier);
    }
}
