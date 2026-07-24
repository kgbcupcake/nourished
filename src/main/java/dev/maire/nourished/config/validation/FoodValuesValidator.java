package dev.maire.nourished.config.validation;

import dev.marie.framework.api.ConfigValidator;
import dev.marie.framework.config.validation.Finding;
import dev.marie.framework.config.validation.ValidationResult;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;

import java.util.ArrayList;
import java.util.List;

public final class FoodValuesValidator implements ConfigValidator {

    private static final String FILE = "food_values.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_food_values";
    }

    @Override
    public ValidationResult validate() {
        if (!FoodValueRegistry.getCategories().isEmpty()) {
            return ValidationResult.pass(validatorId());
        }
        List<Finding> findings = new ArrayList<>();
        findings.add(new Finding(
                ValidationResult.Status.WARN,
                FILE,
                null,
                "FoodValueRegistry has no categories after load"
        ));
        return ValidationResults.fromFindings(validatorId(), findings);
    }
}
