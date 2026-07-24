package dev.maire.nourished.config.validation;

import dev.marie.framework.api.ConfigValidator;
import dev.marie.framework.config.validation.ValidationResult;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;

/**
 * Structural confirmation only. {@link RawFoodConfig#load()} catches I/O errors internally and
 * falls back to bundled defaults via {@code loadDefaults()}, so reaching validation implies load completed.
 */
public final class RawFoodValidator implements ConfigValidator {

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_raw_food";
    }

    @Override
    public ValidationResult validate() {
        // Sanity: tier map is always populated after load (ensureAllTiers / fallbackTier).
        RawFoodConfig.getTier(RawSeverity.SEVERE);
        return ValidationResult.pass(validatorId());
    }
}
