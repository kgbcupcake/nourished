package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.ArrayList;
import java.util.List;

public final class NutrientsValidator implements ConfigValidator {

    private static final String FILE = "nutrients.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_nutrients";
    }

    @Override
    public ValidationResult validate() {
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) {
            List<Finding> findings = new ArrayList<>();
            findings.add(new Finding(
                    ValidationResult.Status.WARN,
                    FILE,
                    null,
                    "NutrientRegistry has no keys after load (expected at least the five built-in groups)"
            ));
            return ValidationResults.fromFindings(validatorId(), findings);
        }
        if (ValidationResults.isSuspiciouslySmallNutrientSet(keys)) {
            List<Finding> findings = new ArrayList<>();
            findings.add(new Finding(
                    ValidationResult.Status.WARN,
                    FILE,
                    null,
                    "Only " + keys.size() + " nutrient key(s) loaded; fewer than the five built-in keys "
                            + "(fruits, vegetables, proteins, grains, dairy)"
            ));
            return ValidationResults.fromFindings(validatorId(), findings);
        }
        return ValidationResult.pass(validatorId());
    }
}
