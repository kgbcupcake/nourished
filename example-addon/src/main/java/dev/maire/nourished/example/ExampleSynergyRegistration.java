package dev.maire.nourished.example;

import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NutrientSynergyDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates both nutrient-to-nutrient synergy and meal-combo (food) synergy registration.
 */
public final class ExampleSynergyRegistration {

    private ExampleSynergyRegistration() {}

    public static void register() {
        NourishedAPI.registerNutrientSynergy(fiberProteinSynergy());
        NourishedAPI.registerFoodSynergy(appleCarrotSynergy());
    }

    private static NutrientSynergyDefinition fiberProteinSynergy() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "fiber_protein_regen");
        fields.put("nutrientKeyA", "fiber");
        fields.put("conditionA", NutrientSynergyDefinition.LevelCondition.OPTIMAL);
        fields.put("nutrientKeyB", "proteins");
        fields.put("conditionB", NutrientSynergyDefinition.LevelCondition.HIGH);
        fields.put("bonusEffectId", ResourceLocation.parse("minecraft:regeneration"));
        fields.put("effectAmplifier", 0);
        fields.put("effectDuration", 100);
        fields.put("isPenalty", false);

        return ExampleApiFactory.instantiate(
                NutrientSynergyDefinition.class,
                "dev.maire.nourished.api.NutrientSynergyDefinition$Builder",
                new Class<?>[]{String.class},
                new Object[]{"fiber_protein_regen"},
                fields
        );
    }

    private static FoodSynergyDefinition appleCarrotSynergy() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "apple_carrot_fiber_boost");
        fields.put("foodA", ResourceLocation.parse("minecraft:apple"));
        fields.put("foodB", ResourceLocation.parse("minecraft:carrot"));
        fields.put("timeWindowTicks", 10 * 20);
        fields.put("bonusNutrientKey", "fiber");
        fields.put("bonusAmount", 0.5f);

        return ExampleApiFactory.instantiate(
                FoodSynergyDefinition.class,
                "dev.maire.nourished.api.FoodSynergyDefinition$Builder",
                new Class<?>[]{String.class},
                new Object[]{"apple_carrot_fiber_boost"},
                fields
        );
    }
}
