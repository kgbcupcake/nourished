package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.marie.MariesLib.runtime.SourceValueRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SourceValuesValidator implements ConfigValidator {

    private static final String FILE = "source_values.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_source_values";
    }

    @Override
    public ValidationResult validate() {
        Set<String> validKeys = Set.copyOf(NutrientRegistry.getKeys());
        List<Finding> findings = new ArrayList<>();

        for (SourceValueRegistry.SourceValueDef def : SourceValueRegistry.getAll()) {
            for (String valueKey : def.multipliers().keySet()) {
                if (!validKeys.contains(valueKey)) {
                    findings.add(new Finding(
                            ValidationResult.Status.WARN,
                            FILE,
                            def.category(),
                            "Unknown nutrient/value key '" + valueKey + "' in category multipliers"
                    ));
                }
            }
        }

        return ValidationResults.fromFindings(validatorId(), findings);
    }
}
