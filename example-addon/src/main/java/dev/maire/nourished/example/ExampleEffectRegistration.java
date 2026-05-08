package dev.maire.nourished.example;

import dev.maire.nourished.api.EffectDefinition;
import dev.maire.nourished.api.NourishedAPI;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates threshold-triggered custom effects for both healthy and critical nutrient states.
 */
public final class ExampleEffectRegistration {

    private ExampleEffectRegistration() {}

    public static void register() {
        NourishedAPI.registerCustomEffect(effect(
                "fiber",
                0.8f,
                EffectDefinition.ThresholdType.EXCESS,
                ResourceLocation.parse("minecraft:speed"),
                0,
                200
        ));

        NourishedAPI.registerCustomEffect(effect(
                "fiber",
                0.15f,
                EffectDefinition.ThresholdType.CRITICAL,
                ResourceLocation.parse("minecraft:slowness"),
                0,
                200
        ));
    }

    private static EffectDefinition effect(
            String nutrientKey,
            float threshold,
            EffectDefinition.ThresholdType thresholdType,
            ResourceLocation effectId,
            int amplifier,
            int duration
    ) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("nutrientKey", nutrientKey);
        fields.put("threshold", threshold);
        fields.put("thresholdType", thresholdType);
        fields.put("effectId", effectId);
        fields.put("amplifier", amplifier);
        fields.put("duration", duration);

        return ExampleApiFactory.instantiate(
                EffectDefinition.class,
                "dev.maire.nourished.api.EffectDefinition$Builder",
                new Class<?>[]{},
                new Object[]{},
                fields
        );
    }
}
