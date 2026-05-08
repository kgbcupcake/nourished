package dev.maire.nourished.example;

import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NutrientDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates custom nutrient registration with display, color, decay, and threshold tuning.
 */
public final class ExampleNutrientRegistration {

    private ExampleNutrientRegistration() {}

    public static void register() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "fiber");
        fields.put("displayName", "Fiber");
        fields.put("color", 0xFF6B8E23);
        fields.put("defaultDecayRate", 0.0014f);
        fields.put("criticalThreshold", 0.15f);
        fields.put("lowThreshold", 0.35f);
        fields.put("excessThreshold", 0.9f);

        NutrientDefinition fiber = ExampleApiFactory.instantiate(
                NutrientDefinition.class,
                "dev.maire.nourished.api.NutrientDefinition$Builder",
                new Class<?>[]{String.class},
                new Object[]{"fiber"},
                fields
        );

        NourishedAPI.registerNutrient(fiber);
    }
}
