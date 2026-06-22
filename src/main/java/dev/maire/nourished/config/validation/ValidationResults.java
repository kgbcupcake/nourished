package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

final class ValidationResults {

    private static final int BUILTIN_NUTRIENT_COUNT = 5;

    private ValidationResults() {}

    static ValidationResult fromFindings(String validatorId, List<Finding> findings) {
        if (findings.isEmpty()) {
            return ValidationResult.pass(validatorId);
        }
        ValidationResult.Status status = ValidationResult.Status.WARN;
        for (Finding finding : findings) {
            if (finding.severity() == ValidationResult.Status.FAIL) {
                status = ValidationResult.Status.FAIL;
                break;
            }
        }
        return new ValidationResult(validatorId, status, List.copyOf(findings));
    }

    static List<Finding> warn(String file, String key, String message) {
        List<Finding> findings = new ArrayList<>(1);
        findings.add(new Finding(ValidationResult.Status.WARN, file, key, message));
        return findings;
    }

    static List<Finding> fail(String file, String key, String message) {
        List<Finding> findings = new ArrayList<>(1);
        findings.add(new Finding(ValidationResult.Status.FAIL, file, key, message));
        return findings;
    }

    static boolean isSuspiciouslySmallNutrientSet(List<String> keys) {
        return keys.size() < BUILTIN_NUTRIENT_COUNT;
    }

    static boolean isKnownNutrientKey(String key, java.util.Set<String> validKeys) {
        return "all".equals(key) || validKeys.contains(key);
    }
}
