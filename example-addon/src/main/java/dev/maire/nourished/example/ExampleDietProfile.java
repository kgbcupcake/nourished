package dev.maire.nourished.example;

import dev.maire.nourished.api.DietProfileDefinition;
import dev.maire.nourished.api.NourishedAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates registration of a named diet profile with custom thresholds and decay overrides.
 */
public final class ExampleDietProfile {

    private ExampleDietProfile() {}

    public static void register() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "herbivore");
        fields.put("displayName", "Herbivore");

        Map<String, Float> thresholds = new HashMap<>();
        thresholds.put("fiber", 0.45f);
        fields.put("customThresholds", thresholds);

        Map<String, Float> decayRates = new HashMap<>();
        decayRates.put("fiber", 0.0009f);
        fields.put("customDecayRates", decayRates);

        DietProfileDefinition profile = ExampleApiFactory.instantiate(
                DietProfileDefinition.class,
                "dev.maire.nourished.api.DietProfileDefinition$Builder",
                new Class<?>[]{String.class},
                new Object[]{"herbivore"},
                fields
        );

        NourishedAPI.registerDietProfile(profile);
    }
}
