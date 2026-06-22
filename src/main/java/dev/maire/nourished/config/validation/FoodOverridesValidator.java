package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FoodOverridesValidator implements ConfigValidator {

    private static final String FILE = "food_overrides.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_food_overrides";
    }

    @Override
    public ValidationResult validate() {
        Set<String> validKeys = Set.copyOf(NutrientRegistry.getKeys());
        List<Finding> findings = new ArrayList<>();

        for (FoodOverrideRegistry.FoodOverride override : FoodOverrideRegistry.getAll().values()) {
            for (String nutrientKey : override.nutrients().keySet()) {
                if (!validKeys.contains(nutrientKey)) {
                    findings.add(new Finding(
                            ValidationResult.Status.WARN,
                            FILE,
                            override.item(),
                            "Unknown nutrient key '" + nutrientKey + "' in food override"
                    ));
                }
            }

            ResourceLocation itemId = ResourceLocation.tryParse(override.item());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                findings.add(new Finding(
                        ValidationResult.Status.WARN,
                        FILE,
                        override.item(),
                        "Item id does not resolve in BuiltInRegistries.ITEM (mod may not be loaded)"
                ));
            }
        }

        return ValidationResults.fromFindings(validatorId(), findings);
    }
}
