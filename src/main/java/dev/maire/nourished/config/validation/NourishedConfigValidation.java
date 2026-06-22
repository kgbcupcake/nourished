package dev.maire.nourished.config.validation;

import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.marie.MariesLib.config.validation.ValidationRunner;
import dev.maire.nourished.core.Nourished;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Runs registered config validators once after {@link dev.marie.MariesLib.registry.RegistryLifecycleManager#loadAll()}.
 */
public final class NourishedConfigValidation {

    private NourishedConfigValidation() {}

    public static void runAfterInitialLoad() {
        List<ValidationResult> results = ValidationRunner.runAll();
        long failCount = results.stream().filter(r -> r.status() == ValidationResult.Status.FAIL).count();
        long warnCount = results.stream().filter(r -> r.status() == ValidationResult.Status.WARN).count();
        if (failCount > 0 || warnCount > 0) {
            Nourished.LOGGER.warn("[Nourished] Config validation: {} FAIL, {} WARN (run /{} validate for details)",
                    failCount, warnCount, Nourished.MODID);
        }
        if (failCount > 0 && FMLEnvironment.dist == Dist.CLIENT) {
            dev.maire.nourished.client.NourishedValidationClient.showFailureToast();
        }
    }
}
