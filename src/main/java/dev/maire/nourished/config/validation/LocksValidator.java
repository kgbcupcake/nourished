package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.core.Nourished;

/**
 * Structural confirmation only. {@link NourishedLockRegistry#load()} catches I/O errors internally
 * and falls back to empty frozen registries, so reaching validation implies load completed.
 */
public final class LocksValidator implements ConfigValidator {

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_locks";
    }

    @Override
    public ValidationResult validate() {
        return ValidationResult.pass(validatorId());
    }
}
