package dev.maire.nourished.modules.Stamina.Core;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import dev.maire.nourished.api.ApiStatus;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * Holds per-player physical and mental stamina state.
 */
@ApiStatus.Internal
public class StaminaData {

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final Decoder<StaminaData> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<StaminaData, T>> decode(DynamicOps<T> ops, T input) {
            return decodeData(ops, input).map(d -> Pair.of(d, input));
        }
    };

    public static final Codec<StaminaData> CODEC = Codec.of(StaminaData::encode, DECODER);

    private static <T> DataResult<T> encode(StaminaData data, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> map = ops.mapBuilder();
        map.add("physical_stamina", Codec.FLOAT.encodeStart(ops, data.physicalStamina));
        map.add("physical_max", Codec.FLOAT.encodeStart(ops, data.physicalMax));
        map.add("physical_fatigue_penalty", Codec.FLOAT.encodeStart(ops, data.physicalFatiguePenalty));
        map.add("physical_bonus_stamina", Codec.FLOAT.encodeStart(ops, data.physicalBonusStamina));
        map.add("physical_debt", Codec.FLOAT.encodeStart(ops, data.physicalDebt));
        map.add("mental_stamina", Codec.FLOAT.encodeStart(ops, data.mentalStamina));
        map.add("mental_max", Codec.FLOAT.encodeStart(ops, data.mentalMax));
        map.add("mental_fatigue_penalty", Codec.FLOAT.encodeStart(ops, data.mentalFatiguePenalty));
        map.add("mental_bonus_stamina", Codec.FLOAT.encodeStart(ops, data.mentalBonusStamina));
        map.add("mental_debt", Codec.FLOAT.encodeStart(ops, data.mentalDebt));
        map.add("last_update_ms", Codec.LONG.encodeStart(ops, data.lastUpdateMs));
        return map.build(prefix);
    }

    private static <T> DataResult<StaminaData> decodeData(DynamicOps<T> ops, T input) {
        MapLike<T> map = ops.getMap(input).getOrThrow();

        StaminaData data = new StaminaData();
        data.physicalStamina = decodeFloat(ops, map, "physical_stamina", StaminaConfig.initialPhysicalMax());
        data.physicalMax = decodeFloat(ops, map, "physical_max", StaminaConfig.initialPhysicalMax());
        data.physicalFatiguePenalty = decodeFloat(ops, map, "physical_fatigue_penalty", 0.0f);
        data.physicalBonusStamina = decodeFloat(ops, map, "physical_bonus_stamina", 0.0f);
        data.physicalDebt = decodeFloat(ops, map, "physical_debt", 0.0f);
        data.mentalStamina = decodeFloat(ops, map, "mental_stamina", StaminaConfig.initialMentalMax());
        data.mentalMax = decodeFloat(ops, map, "mental_max", StaminaConfig.initialMentalMax());
        data.mentalFatiguePenalty = decodeFloat(ops, map, "mental_fatigue_penalty", 0.0f);
        data.mentalBonusStamina = decodeFloat(ops, map, "mental_bonus_stamina", 0.0f);
        data.mentalDebt = decodeFloat(ops, map, "mental_debt", 0.0f);
        data.lastUpdateMs = decodeLong(ops, map, "last_update_ms", 0L);

        data.clampCoreValues();
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

    private float physicalStamina;
    private float physicalMax;
    private float physicalFatiguePenalty;
    private float physicalBonusStamina;
    private float physicalDebt;

    private float mentalStamina;
    private float mentalMax;
    private float mentalFatiguePenalty;
    private float mentalBonusStamina;
    private float mentalDebt;

    private long lastUpdateMs;

    public StaminaData() {
        this.physicalStamina = StaminaConfig.initialPhysicalMax();
        this.physicalMax = StaminaConfig.initialPhysicalMax();
        this.physicalFatiguePenalty = 0.0f;
        this.physicalBonusStamina = 0.0f;
        this.physicalDebt = 0.0f;
        this.mentalStamina = StaminaConfig.initialMentalMax();
        this.mentalMax = StaminaConfig.initialMentalMax();
        this.mentalFatiguePenalty = 0.0f;
        this.mentalBonusStamina = 0.0f;
        this.mentalDebt = 0.0f;
        this.lastUpdateMs = 0L;
    }

    /**
     * Creates a StaminaData instance from sync payload values.
     * Used on the client to apply server state.
     */
    public static StaminaData fromSync(
            float physicalStamina,
            float physicalMax,
            float physicalFatiguePenalty,
            float physicalBonusStamina,
            float physicalDebt,
            float mentalStamina,
            float mentalMax,
            float mentalFatiguePenalty,
            float mentalBonusStamina,
            float mentalDebt
    ) {
        StaminaData data = new StaminaData();
        data.physicalMax = Mth.clamp(physicalMax, StaminaConfig.minStamina(), StaminaConfig.maxStamina());
        data.physicalFatiguePenalty = Mth.clamp(physicalFatiguePenalty, 0.0f, StaminaConfig.maxFatiguePenalty());
        data.physicalBonusStamina = Math.max(0.0f, physicalBonusStamina);
        data.physicalDebt = Mth.clamp(physicalDebt, 0.0f, StaminaConfig.maxDebt());
        data.physicalStamina = Mth.clamp(physicalStamina, 0.0f, data.effectivePhysicalMax());

        data.mentalMax = Mth.clamp(mentalMax, StaminaConfig.minStamina(), StaminaConfig.maxStamina());
        data.mentalFatiguePenalty = Mth.clamp(mentalFatiguePenalty, 0.0f, StaminaConfig.maxFatiguePenalty());
        data.mentalBonusStamina = Math.max(0.0f, mentalBonusStamina);
        data.mentalDebt = Mth.clamp(mentalDebt, 0.0f, StaminaConfig.maxDebt());
        data.mentalStamina = Mth.clamp(mentalStamina, 0.0f, data.effectiveMentalMax());
        return data;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public float getPhysicalStamina() {
        return physicalStamina;
    }

    public float getPhysicalMax() {
        return physicalMax;
    }

    public float getPhysicalFatiguePenalty() {
        return physicalFatiguePenalty;
    }

    public float getPhysicalBonusStamina() {
        return physicalBonusStamina;
    }

    public float getPhysicalDebt() {
        return physicalDebt;
    }

    public float getMentalStamina() {
        return mentalStamina;
    }

    public float getMentalMax() {
        return mentalMax;
    }

    public float getMentalFatiguePenalty() {
        return mentalFatiguePenalty;
    }

    public float getMentalBonusStamina() {
        return mentalBonusStamina;
    }

    public float getMentalDebt() {
        return mentalDebt;
    }

    public long getLastUpdateMs() {
        return lastUpdateMs;
    }

    public void setLastUpdateMs(long ms) {
        this.lastUpdateMs = ms;
    }

    // ── Derived Values ───────────────────────────────────────────────────────

    public float effectivePhysicalMax() {
        return Math.max(StaminaConfig.minStamina(), physicalMax - physicalFatiguePenalty);
    }

    public float effectiveMentalMax() {
        return Math.max(StaminaConfig.minStamina(), mentalMax - mentalFatiguePenalty);
    }

    public float effectivePhysicalStamina() {
        return Mth.clamp(physicalStamina + physicalBonusStamina, 0.0f, effectivePhysicalMax() + physicalBonusStamina);
    }

    public float effectiveMentalStamina() {
        return Mth.clamp(mentalStamina + mentalBonusStamina, 0.0f, effectiveMentalMax() + mentalBonusStamina);
    }

    public boolean isPhysicalExhausted() {
        return physicalStamina <= 0.0f && physicalBonusStamina <= 0.0f;
    }

    public boolean isMentalExhausted() {
        return mentalStamina <= 0.0f && mentalBonusStamina <= 0.0f;
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    public void drainPhysical(float amount) {
        if (amount <= 0.0f) {
            return;
        }

        float remaining = amount;
        float bonusDrain = Math.min(physicalBonusStamina, remaining);
        physicalBonusStamina = Math.max(0.0f, physicalBonusStamina - remaining);
        remaining -= bonusDrain;

        float staminaDrain = Math.min(physicalStamina, remaining);
        physicalStamina = Math.max(0.0f, physicalStamina - remaining);
        remaining -= staminaDrain;

        if (physicalStamina <= 0.0f && remaining > 0.0f) {
            physicalDebt = Mth.clamp(physicalDebt + remaining, 0.0f, StaminaConfig.maxDebt());
        }
    }

    public void drainMental(float amount) {
        if (amount <= 0.0f) {
            return;
        }

        float remaining = amount;
        float bonusDrain = Math.min(mentalBonusStamina, remaining);
        mentalBonusStamina = Math.max(0.0f, mentalBonusStamina - remaining);
        remaining -= bonusDrain;

        float staminaDrain = Math.min(mentalStamina, remaining);
        mentalStamina = Math.max(0.0f, mentalStamina - remaining);
        remaining -= staminaDrain;

        if (mentalStamina <= 0.0f && remaining > 0.0f) {
            mentalDebt = Mth.clamp(mentalDebt + remaining, 0.0f, StaminaConfig.maxDebt());
        }
    }

    public void regenPhysical(float amount) {
        if (amount <= 0.0f) {
            return;
        }

        float remaining = repayPhysicalDebt(amount);
        physicalStamina = Mth.clamp(physicalStamina + remaining, 0.0f, effectivePhysicalMax());
    }

    public void regenMental(float amount) {
        if (amount <= 0.0f) {
            return;
        }

        float remaining = repayMentalDebt(amount);
        mentalStamina = Mth.clamp(mentalStamina + remaining, 0.0f, effectiveMentalMax());
    }

    public void addPhysicalBonus(float amount) {
        if (amount > 0.0f) {
            physicalBonusStamina += amount;
        }
    }

    public void addMentalBonus(float amount) {
        if (amount > 0.0f) {
            mentalBonusStamina += amount;
        }
    }

    public void decayPhysicalBonus(float rate) {
        physicalBonusStamina = Math.max(0.0f, physicalBonusStamina - Math.max(0.0f, rate));
    }

    public void decayMentalBonus(float rate) {
        mentalBonusStamina = Math.max(0.0f, mentalBonusStamina - Math.max(0.0f, rate));
    }

    public void applyFatigue(boolean physical, float amount) {
        if (amount <= 0.0f) {
            return;
        }
        if (physical) {
            physicalFatiguePenalty = Mth.clamp(physicalFatiguePenalty + amount, 0.0f, StaminaConfig.maxFatiguePenalty());
        } else {
            mentalFatiguePenalty = Mth.clamp(mentalFatiguePenalty + amount, 0.0f, StaminaConfig.maxFatiguePenalty());
        }
    }

    public void recoverFatigue(boolean physical, float rate) {
        if (rate <= 0.0f) {
            return;
        }
        if (physical) {
            physicalFatiguePenalty = Math.max(0.0f, physicalFatiguePenalty - rate);
        } else {
            mentalFatiguePenalty = Math.max(0.0f, mentalFatiguePenalty - rate);
        }
    }

    public void applyNutritionModifiers(Map<String, Float> nutrients, float gutHealth) {
        float basePhysical = StaminaConfig.initialPhysicalMax();
        float baseMental = StaminaConfig.initialMentalMax();
        float proteinValue = Mth.clamp(nutrients.getOrDefault("proteins", 0.0f), 0.0f, 1.0f);
        float grainValue = Mth.clamp(nutrients.getOrDefault("grains", 0.0f), 0.0f, 1.0f);
        float gutValue = Mth.clamp(gutHealth, 0.0f, 1.0f);

        float proteinModifier = Mth.lerp(proteinValue, StaminaConfig.minNutritionModifier(), StaminaConfig.maxNutritionModifier());
        float grainModifier = Mth.lerp(grainValue, StaminaConfig.minNutritionModifier(), StaminaConfig.maxNutritionModifier());
        float gutModifier = Mth.lerp(gutValue, StaminaConfig.minGutModifier(), 1.0f);

        physicalMax = Mth.clamp(basePhysical * proteinModifier * gutModifier, StaminaConfig.minStamina(), StaminaConfig.maxStamina());
        mentalMax = Mth.clamp(baseMental * grainModifier * gutModifier, StaminaConfig.minStamina(), StaminaConfig.maxStamina());
        physicalStamina = Mth.clamp(physicalStamina, 0.0f, effectivePhysicalMax());
        mentalStamina = Mth.clamp(mentalStamina, 0.0f, effectiveMentalMax());
    }

    private float repayPhysicalDebt(float amount) {
        if (physicalDebt <= 0.0f) {
            return amount;
        }

        float repayment = Math.min(physicalDebt, amount * StaminaConfig.debtRepayRate());
        physicalDebt = Math.max(0.0f, physicalDebt - repayment);
        return Math.max(0.0f, amount - repayment);
    }

    private float repayMentalDebt(float amount) {
        if (mentalDebt <= 0.0f) {
            return amount;
        }

        float repayment = Math.min(mentalDebt, amount * StaminaConfig.debtRepayRate());
        mentalDebt = Math.max(0.0f, mentalDebt - repayment);
        return Math.max(0.0f, amount - repayment);
    }

    private void clampCoreValues() {
        physicalMax = Mth.clamp(physicalMax, StaminaConfig.minStamina(), StaminaConfig.maxStamina());
        physicalFatiguePenalty = Mth.clamp(physicalFatiguePenalty, 0.0f, StaminaConfig.maxFatiguePenalty());
        physicalBonusStamina = Math.max(0.0f, physicalBonusStamina);
        physicalDebt = Mth.clamp(physicalDebt, 0.0f, StaminaConfig.maxDebt());
        physicalStamina = Mth.clamp(physicalStamina, 0.0f, effectivePhysicalMax());

        mentalMax = Mth.clamp(mentalMax, StaminaConfig.minStamina(), StaminaConfig.maxStamina());
        mentalFatiguePenalty = Mth.clamp(mentalFatiguePenalty, 0.0f, StaminaConfig.maxFatiguePenalty());
        mentalBonusStamina = Math.max(0.0f, mentalBonusStamina);
        mentalDebt = Mth.clamp(mentalDebt, 0.0f, StaminaConfig.maxDebt());
        mentalStamina = Mth.clamp(mentalStamina, 0.0f, effectiveMentalMax());
    }
}
