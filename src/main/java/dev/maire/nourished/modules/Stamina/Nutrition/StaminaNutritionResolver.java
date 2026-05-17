package dev.maire.nourished.modules.Stamina.Nutrition;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthData;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;

@ApiStatus.Internal
public final class StaminaNutritionResolver {

    private StaminaNutritionResolver() {}

    public static StaminaNutritionProfile resolve(DietData diet, GutHealthData gut) {
        float proteins = diet.nutrients.getOrDefault("proteins", 0f);
        float grains = diet.nutrients.getOrDefault("grains", 0f);
        float vegetables = diet.nutrients.getOrDefault("vegetables", 0f);
        float dairy = diet.nutrients.getOrDefault("dairy", 0f);
        float gutHealth = gut.getGutHealth();
        float balance = diet.getBalanceScore();

        float proteinMod = lerp(
                StaminaConfig.minNutritionModifier(),
                StaminaConfig.maxNutritionModifier(),
                proteins
        );
        float gutMod = lerp(StaminaConfig.minGutModifier(), 1.0f, gutHealth);
        float physicalModifier = proteinMod * gutMod;

        float grainMod = lerp(
                StaminaConfig.minNutritionModifier(),
                StaminaConfig.maxNutritionModifier(),
                grains
        );
        float mentalModifier = grainMod * gutMod;

        float regenModifier = lerp(
                StaminaConfig.minNutritionModifier(),
                StaminaConfig.maxNutritionModifier(),
                balance
        );

        // TODO: move weights to StaminaConfig in a future pass.
        float fatigueResistance = Math.max(0f, Math.min(1f,
                (dairy * 0.6f) + (vegetables * 0.4f)
        ));

        return new StaminaNutritionProfile(
                proteins, grains, vegetables, dairy, gutHealth, balance,
                physicalModifier, mentalModifier, regenModifier, fatigueResistance
        );
    }

    private static float lerp(float min, float max, float t) {
        return min + (max - min) * Math.max(0f, Math.min(1f, t));
    }
}
