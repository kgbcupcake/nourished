package dev.maire.nourished.modules.RawFood.rawInfo;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * Resolves a resistance float in [0.0, 1.0] for a given player and severity tier.
 * Higher resistance = less penalty applied.
 *
 * <p>This is NOT cached — nutrient values change constantly. This is a pure
 * calculation on the hot path, but it's just a map lookup and a float multiply
 * so performance is fine.</p>
 */
@ApiStatus.Internal
public final class RawFoodResistanceResolver {

    private RawFoodResistanceResolver() {}

    /**
     * Resolves resistance for a player eating raw food of the given severity.
     *
     * @param player   the player eating raw food
     * @param severity the raw food severity tier
     * @return resistance float in [0.0, 1.0] where higher means less penalty
     */
    public static float resolve(ServerPlayer player, RawSeverity severity) {
        if (severity == RawSeverity.FINE) {
            return 1.0f;
        }

        TrackingData diet = player.getData(TrackingAttachment.TRACKING.get());
        RawFoodResistanceConfig config = RawFoodConfig.getResistanceConfig(severity);

        float totalResistance = 0.0f;
        for (Map.Entry<String, Float> entry : config.nutrientWeights().entrySet()) {
            String valueKey = entry.getKey();
            float weight = entry.getValue();
            float currentValue = diet.values.getOrDefault(valueKey, 0f);
            if (currentValue >= config.resistanceThreshold()) {
                totalResistance += weight;
            }
        }

        return Mth.clamp(totalResistance, 0.0f, config.maxResistance());
    }
}
