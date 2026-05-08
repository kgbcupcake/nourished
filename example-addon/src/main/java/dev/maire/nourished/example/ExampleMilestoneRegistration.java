package dev.maire.nourished.example;

import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates cumulative nutrient milestone registration with a one-time reward effect.
 */
public final class ExampleMilestoneRegistration {

    private ExampleMilestoneRegistration() {}

    public static void register() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "fiber_centurion");
        fields.put("nutrientKey", "fiber");
        fields.put("cumulativeGoal", 100.0f);
        fields.put("rewardEffectId", ResourceLocation.parse("minecraft:night_vision"));
        fields.put("rewardAmplifier", 0);
        fields.put("rewardDuration", 30 * 20);

        NutrientMilestoneDefinition milestone = ExampleApiFactory.instantiate(
                NutrientMilestoneDefinition.class,
                "dev.maire.nourished.api.NutrientMilestoneDefinition$Builder",
                new Class<?>[]{String.class},
                new Object[]{"fiber_centurion"},
                fields
        );

        // Milestones are persistent by design in Nourished and fire once per player.
        NourishedAPI.registerMilestone(milestone);
    }
}
