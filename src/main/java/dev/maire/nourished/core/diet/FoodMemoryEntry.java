package dev.maire.nourished.core.diet;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.maire.nourished.api.ApiStatus;

/**
 * Tracks how many times a specific food has been eaten and when.
 * <p>
 * SCHEMA CHANGE v2: eatCount is now float (supports fractional streak increments),
 * lastEatenMs renamed to lastEatenTick for semantic clarity (still stores ms for now).
 * Codec is backward-compatible: reads int OR float for eatCount, falls back to 0L for missing tick.
 */
@ApiStatus.Internal
public record FoodMemoryEntry(float eatCount, long lastEatenTick) {

    /**
     * Grace period (ms) before exponential decay begins after an eat. Within this window,
     * {@link #decayedEatCount(long, long)} returns the stored count unchanged so that rapid
     * eating sprees fully accumulate before decay starts eroding them. Decay timing is then
     * measured from the end of the grace period, not from {@link #lastEatenTick}.
     */
    private static final long DECAY_GRACE_MS = 60_000L;

    public static final Codec<FoodMemoryEntry> CODEC = Codec.of(
            FoodMemoryEntry::encode,
            FoodMemoryEntry::decode
    );

    private static <T> DataResult<T> encode(FoodMemoryEntry entry, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> builder = ops.mapBuilder();
        builder.add("eatCount", Codec.FLOAT.encodeStart(ops, entry.eatCount));
        builder.add("lastEatenTick", Codec.LONG.encodeStart(ops, entry.lastEatenTick));
        return builder.build(prefix);
    }

    private static <T> DataResult<Pair<FoodMemoryEntry, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).flatMap(map -> {
            float eatCount = decodeEatCount(ops, map);
            long lastEatenTick = decodeLong(ops, map, "lastEatenTick",
                    decodeLong(ops, map, "lastEatenMs", 0L)); // fallback to old field name
            return DataResult.success(Pair.of(new FoodMemoryEntry(eatCount, lastEatenTick), input));
        });
    }

    private static <T> float decodeEatCount(DynamicOps<T> ops, MapLike<T> map) {
        T val = map.get("eatCount");
        if (val == null) return 0f;
        // Try float first, then int for backward compatibility
        return Codec.FLOAT.parse(ops, val).result()
                .orElseGet(() -> Codec.INT.parse(ops, val).result()
                        .map(Integer::floatValue)
                        .orElse(0f));
    }

    private static <T> long decodeLong(DynamicOps<T> ops, MapLike<T> map, String field, long fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.LONG.parse(ops, val).result().orElse(fallback);
    }

    /**
     * Computes the effective eat count after exponential decay.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If {@code halfLifeMs <= 0}, decay is disabled and the stored count is returned as-is.</li>
     *   <li>For the first {@link #DECAY_GRACE_MS} after {@link #lastEatenTick}, no decay is applied.
     *       This protects rapid eating sprees so the count can accumulate fully before erosion begins.</li>
     *   <li>After the grace period, decay is exponential with the given half-life, measured from the
     *       end of the grace window: {@code eatCount * 2^(-(elapsed - grace) / halfLife)}.</li>
     * </ul>
     *
     * @param halfLifeMs  time in ms for eat count to halve once decay starts
     * @param currentTimeMs current game time in ms
     * @return decayed eat count, always >= 0
     */
    public float decayedEatCount(long halfLifeMs, long currentTimeMs) {
        if (halfLifeMs <= 0) return eatCount;
        long elapsed = currentTimeMs - lastEatenTick;
        if (elapsed <= DECAY_GRACE_MS) return eatCount;
        double decayFactor = Math.pow(2.0, -(double) (elapsed - DECAY_GRACE_MS) / halfLifeMs);
        return (float) (eatCount * decayFactor);
    }

    /**
     * Streak-aware eat recording. If eaten within streak window, applies bonus weight.
     *
     * @param streakWindowMs  time window for streak bonus
     * @param streakWeight    multiplier for increment when within streak (e.g., 2.0 = double increment)
     * @param currentTimeMs   current game time in ms
     * @return new entry with updated count and timestamp
     */
    public FoodMemoryEntry withEat(long streakWindowMs, float streakWeight, long currentTimeMs) {
        long elapsed = currentTimeMs - lastEatenTick;
        float increment = (elapsed <= streakWindowMs && elapsed >= 0) ? streakWeight : 1.0f;
        return new FoodMemoryEntry(eatCount + increment, currentTimeMs);
    }

    /**
     * Check if this entry has effectively expired (decayed below threshold).
     *
     * @param halfLifeMs    half-life for decay calculation
     * @param currentTimeMs current time
     * @param threshold     decay threshold (e.g., 0.1f)
     * @return true if decayed count is below threshold
     */
    public boolean isEffectivelyExpired(long halfLifeMs, long currentTimeMs, float threshold) {
        return decayedEatCount(halfLifeMs, currentTimeMs) < threshold;
    }

    /**
     * Legacy expiration check for backward compatibility.
     * @deprecated Use {@link #isEffectivelyExpired(long, long, float)} with decay instead
     */
    @Deprecated
    public boolean isExpired(long windowMs) {
        return System.currentTimeMillis() - lastEatenTick > windowMs;
    }
}
