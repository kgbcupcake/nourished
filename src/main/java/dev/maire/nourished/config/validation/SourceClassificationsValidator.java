package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.marie.MariesLib.runtime.SourceClassificationRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SourceClassificationsValidator implements ConfigValidator {

    private static final String FILE = "source_classifications.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_source_classifications";
    }

    @Override
    public ValidationResult validate() {
        Set<String> validKeys = Set.copyOf(NutrientRegistry.getKeys());
        List<Finding> findings = new ArrayList<>();

        for (SourceClassificationRegistry.SourceClassification entry : SourceClassificationRegistry.getAll().values()) {
            for (String valueKey : entry.values().keySet()) {
                if (!validKeys.contains(valueKey)) {
                    findings.add(new Finding(
                            ValidationResult.Status.WARN,
                            FILE,
                            entry.sourceId(),
                            "Unknown nutrient/value key '" + valueKey + "' in source classification values"
                    ));
                }
            }
        }

        return ValidationResults.fromFindings(validatorId(), findings);
    }
}
