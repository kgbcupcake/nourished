package dev.maire.nourished.core.network.sync;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncNourishedConfigSnapshot(
        int protocolVersion,
        double decayRate,
        int decayIntervalTicks,
        double criticalThreshold,
        double lowThreshold,
        double excessThreshold,
        double bonusEffectThreshold,
        double penaltyEffectThreshold,
        double startingNutrientValue,
        boolean enableRawFoodPenalty,
        boolean enableEffects,
        boolean enableHUD,
        boolean enableToasts,
        double confidenceSpreadThreshold,
        float compositeRatioThreshold,
        Map<String, Double> nutrientDecayOverrides
) implements CustomPacketPayload {

    public static final int PROTOCOL_VERSION = 1;

    public static final CustomPacketPayload.Type<SyncNourishedConfigSnapshot> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_config_snapshot"));

    private static final StreamCodec<FriendlyByteBuf, Map<String, Double>> DECAY_OVERRIDES_CODEC =
            StreamCodec.of(
                    (buf, map) -> {
                        buf.writeVarInt(map.size());
                        map.forEach((k, v) -> {
                            buf.writeUtf(k);
                            buf.writeDouble(v);
                        });
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        Map<String, Double> map = new HashMap<>(Math.max(8, size * 2));
                        for (int i = 0; i < size; i++) {
                            map.put(buf.readUtf(), buf.readDouble());
                        }
                        return map;
                    }
            );

    public static final StreamCodec<FriendlyByteBuf, SyncNourishedConfigSnapshot> STREAM_CODEC =
            StreamCodec.of(
                    (buf, s) -> {
                        buf.writeVarInt(s.protocolVersion());
                        buf.writeDouble(s.decayRate());
                        buf.writeVarInt(s.decayIntervalTicks());
                        buf.writeDouble(s.criticalThreshold());
                        buf.writeDouble(s.lowThreshold());
                        buf.writeDouble(s.excessThreshold());
                        buf.writeDouble(s.bonusEffectThreshold());
                        buf.writeDouble(s.penaltyEffectThreshold());
                        buf.writeDouble(s.startingNutrientValue());
                        buf.writeBoolean(s.enableRawFoodPenalty());
                        buf.writeBoolean(s.enableEffects());
                        buf.writeBoolean(s.enableHUD());
                        buf.writeBoolean(s.enableToasts());
                        buf.writeDouble(s.confidenceSpreadThreshold());
                        buf.writeFloat(s.compositeRatioThreshold());
                        DECAY_OVERRIDES_CODEC.encode(buf, s.nutrientDecayOverrides());
                    },
                    buf -> {
                        int version = buf.readVarInt();
                        double decayRate = buf.readDouble();
                        int decayIntervalTicks = buf.readVarInt();
                        double criticalThreshold = buf.readDouble();
                        double lowThreshold = buf.readDouble();
                        double excessThreshold = buf.readDouble();
                        double bonusEffectThreshold = buf.readDouble();
                        double penaltyEffectThreshold = buf.readDouble();
                        double startingNutrientValue = buf.readDouble();
                        boolean enableRawFoodPenalty = buf.readBoolean();
                        boolean enableEffects = buf.readBoolean();
                        boolean enableHUD = buf.readBoolean();
                        boolean enableToasts = buf.readBoolean();
                        double confidenceSpreadThreshold = buf.readDouble();
                        float compositeRatioThreshold = buf.readFloat();
                        Map<String, Double> overrides = DECAY_OVERRIDES_CODEC.decode(buf);
                        return new SyncNourishedConfigSnapshot(
                                version, decayRate, decayIntervalTicks, criticalThreshold,
                                lowThreshold, excessThreshold, bonusEffectThreshold,
                                penaltyEffectThreshold, startingNutrientValue, enableRawFoodPenalty,
                                enableEffects, enableHUD, enableToasts, confidenceSpreadThreshold,
                                compositeRatioThreshold, overrides
                        );
                    }
            );

    public static SyncNourishedConfigSnapshot fromConfig(NourishedConfig config) {
        Map<String, Double> overrides = new HashMap<>();
        config.nutrientDecayRateOverrides().forEach((key, val) -> {
            double v = val.get();
            if (v >= 0.0) {
                overrides.put(key, v);
            }
        });
        return new SyncNourishedConfigSnapshot(
                PROTOCOL_VERSION,
                config.decayRate(),
                config.decayIntervalTicks(),
                config.criticalThreshold(),
                config.lowThreshold(),
                config.excessThreshold(),
                config.bonusEffectThreshold(),
                config.penaltyEffectThreshold(),
                config.startingNutrientValue(),
                config.isModuleEnabled("enableRawFoodPenalty"),
                config.enableEffects(),
                config.enableHUD(),
                config.enableToasts(),
                config.scannerConfidenceSpreadThreshold(),
                config.compositeRatioThreshold(),
                overrides
        );
    }

    /** Resolves decay rate: per-nutrient override takes precedence over global. */
    public double decayRateFor(String key) {
        Double override = nutrientDecayOverrides.get(key);
        return override != null ? override : decayRate;
    }

    @Override
    public CustomPacketPayload.Type<SyncNourishedConfigSnapshot> type() {
        return TYPE;
    }
}
