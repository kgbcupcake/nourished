package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.marie.MariesLib.scanner.ArchetypePattern;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScannerSpecValidator implements ConfigValidator {

    private static final String FILE = "scanner_spec.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_scanner_spec";
    }

    @Override
    public ValidationResult validate() {
        ScannerSpecRegistry.ScannerSpec spec = ScannerSpecRegistry.get();
        List<Finding> findings = new ArrayList<>();
        Set<String> validKeys = Set.copyOf(NutrientRegistry.getKeys());

        if (isEffectivelyEmpty(spec)) {
            findings.add(new Finding(
                    ValidationResult.Status.FAIL,
                    FILE,
                    null,
                    "Scanner spec is effectively empty (ScannerSpec.empty() fallback); "
                            + "items will misclassify (e.g. defaulting to fruits/fiber)"
            ));
        }

        checkNestedNutrientKeys(spec.keywordWeights(), "keywords", validKeys, findings);
        checkNestedNutrientKeys(spec.suffixWeights(), "suffixes", validKeys, findings);
        checkNestedNutrientKeys(spec.namespaceWeights(), "namespaces", validKeys, findings);
        checkNestedNutrientKeys(spec.communityTagWeights(), "community_tags", validKeys, findings);

        for (ArchetypePattern archetype : spec.archetypes()) {
            for (String nutrientKey : archetype.contributions().keySet()) {
                if (!validKeys.contains(nutrientKey)) {
                    findings.add(new Finding(
                            ValidationResult.Status.WARN,
                            FILE,
                            archetype.pattern(),
                            "Unknown nutrient key '" + nutrientKey + "' in archetype contributions"
                    ));
                }
            }
        }

        return ValidationResults.fromFindings(validatorId(), findings);
    }

    private static boolean isEffectivelyEmpty(ScannerSpecRegistry.ScannerSpec spec) {
        return spec.keywordWeights().isEmpty()
                && spec.suffixWeights().isEmpty()
                && spec.namespaceWeights().isEmpty()
                && spec.communityTagWeights().isEmpty()
                && spec.archetypes().isEmpty();
    }

    private static void checkNestedNutrientKeys(
            Map<String, Map<String, Float>> outer,
            String sectionLabel,
            Set<String> validKeys,
            List<Finding> findings
    ) {
        for (Map.Entry<String, Map<String, Float>> entry : outer.entrySet()) {
            String outerKey = entry.getKey();
            for (String nutrientKey : entry.getValue().keySet()) {
                if (!validKeys.contains(nutrientKey)) {
                    findings.add(new Finding(
                            ValidationResult.Status.WARN,
                            FILE,
                            outerKey,
                            "Unknown nutrient key '" + nutrientKey + "' in " + sectionLabel
                    ));
                }
            }
        }
    }
}
