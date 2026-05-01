package dev.maire.nourished.config;

import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NourishedConfig {

    private static NourishedConfig INSTANCE;
    private static ModConfigSpec SPEC;

    private final ModConfigSpec.DoubleValue decayRate;
    private final ModConfigSpec.IntValue decayIntervalTicks;
    private final ModConfigSpec.BooleanValue enableEffects;
    private final ModConfigSpec.BooleanValue enableHUD;

    private final ModConfigSpec.DoubleValue criticalThreshold;
    private final ModConfigSpec.DoubleValue lowThreshold;
    private final ModConfigSpec.DoubleValue excessThreshold;

    private final ModConfigSpec.DoubleValue bonusEffectThreshold;
    private final ModConfigSpec.DoubleValue penaltyEffectThreshold;

    private final Map<String, ModConfigSpec.DoubleValue> nutrientDecayRateOverrides;
    private final Map<String, ModConfigSpec.DoubleValue> nutrientCriticalThresholdOverrides;

    private NourishedConfig(ModConfigSpec.Builder builder) {
        builder.push("general");
        decayRate = builder.defineInRange("decayRate", 0.1d, 0.0d, 1.0d);
        decayIntervalTicks = builder.defineInRange("decayIntervalTicks", 1200, 1, Integer.MAX_VALUE);
        enableEffects = builder.define("enableEffects", true);
        enableHUD = builder.define("enableHUD", true);
        builder.pop();

        builder.push("thresholds");
        criticalThreshold = builder.defineInRange("criticalThreshold", 0.25d, 0.0d, 1.0d);
        lowThreshold = builder.defineInRange("lowThreshold", 0.40d, 0.0d, 1.0d);
        excessThreshold = builder.defineInRange("excessThreshold", 0.90d, 0.0d, 1.0d);
        builder.pop();

        builder.push("effects");
        bonusEffectThreshold = builder.defineInRange("bonusEffectThreshold", 0.75d, 0.0d, 1.0d);
        penaltyEffectThreshold = builder.defineInRange("penaltyEffectThreshold", 0.25d, 0.0d, 1.0d);
        builder.pop();

        nutrientDecayRateOverrides = new LinkedHashMap<>();
        nutrientCriticalThresholdOverrides = new LinkedHashMap<>();
        builder.push("nutrients");
        for (String key : NutrientRegistry.getKeys()) {
            builder.push(key);
            nutrientDecayRateOverrides.put(
                    key,
                    builder.defineInRange(key + "_decayRate", -1.0d, -1.0d, 1.0d)
            );
            nutrientCriticalThresholdOverrides.put(
                    key,
                    builder.defineInRange(key + "_criticalThreshold", -1.0d, -1.0d, 1.0d)
            );
            builder.pop();
        }
        builder.pop();
    }

    public static void register(ModContainer modContainer) {
        if (INSTANCE != null) return;
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new NourishedConfig(builder);
        SPEC = builder.build();
        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static NourishedConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NourishedConfig has not been registered yet.");
        }
        return INSTANCE;
    }

    public static ModConfigSpec spec() {
        return SPEC;
    }

    public double decayRate() {
        return decayRate.get();
    }

    public void setDecayRate(double value) {
        decayRate.set(value);
    }

    public int decayIntervalTicks() {
        return decayIntervalTicks.get();
    }

    public void setDecayIntervalTicks(int value) {
        decayIntervalTicks.set(value);
    }

    public boolean enableEffects() {
        return enableEffects.get();
    }

    public void setEnableEffects(boolean value) {
        enableEffects.set(value);
    }

    public boolean enableHUD() {
        return enableHUD.get();
    }

    public void setEnableHUD(boolean value) {
        enableHUD.set(value);
    }

    public double criticalThreshold() {
        return criticalThreshold.get();
    }

    public void setCriticalThreshold(double value) {
        criticalThreshold.set(value);
    }

    public double lowThreshold() {
        return lowThreshold.get();
    }

    public void setLowThreshold(double value) {
        lowThreshold.set(value);
    }

    public double excessThreshold() {
        return excessThreshold.get();
    }

    public void setExcessThreshold(double value) {
        excessThreshold.set(value);
    }

    public double bonusEffectThreshold() {
        return bonusEffectThreshold.get();
    }

    public void setBonusEffectThreshold(double value) {
        bonusEffectThreshold.set(value);
    }

    public double penaltyEffectThreshold() {
        return penaltyEffectThreshold.get();
    }

    public void setPenaltyEffectThreshold(double value) {
        penaltyEffectThreshold.set(value);
    }

    public double decayRateFor(String key) {
        ModConfigSpec.DoubleValue value = nutrientDecayRateOverrides.get(key);
        if (value == null || value.get() < 0d) return decayRate();
        return value.get();
    }

    public double criticalThresholdFor(String key) {
        ModConfigSpec.DoubleValue value = nutrientCriticalThresholdOverrides.get(key);
        if (value == null || value.get() < 0d) return criticalThreshold();
        return value.get();
    }

    public Map<String, ModConfigSpec.DoubleValue> nutrientDecayRateOverrides() {
        return nutrientDecayRateOverrides;
    }

    public Map<String, ModConfigSpec.DoubleValue> nutrientCriticalThresholdOverrides() {
        return nutrientCriticalThresholdOverrides;
    }
}
