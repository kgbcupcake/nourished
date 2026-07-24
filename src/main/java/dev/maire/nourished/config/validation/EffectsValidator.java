package dev.maire.nourished.config.validation;

import dev.marie.framework.api.ConfigValidator;
import dev.marie.framework.config.validation.Finding;
import dev.marie.framework.config.validation.ValidationResult;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EffectsValidator implements ConfigValidator {

    private static final String FILE = "effects.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_effects";
    }

    @Override
    public ValidationResult validate() {
        Set<String> validKeys = Set.copyOf(NutrientRegistry.getKeys());
        List<Finding> findings = new ArrayList<>();

        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            String nutrient = def.nutrient();
            if (!ValidationResults.isKnownNutrientKey(nutrient, validKeys)) {
                findings.add(new Finding(
                        ValidationResult.Status.WARN,
                        FILE,
                        def.id(),
                        "Unknown nutrient key '" + nutrient + "' on effect definition"
                ));
            }
        }

        return ValidationResults.fromFindings(validatorId(), findings);
    }
}
