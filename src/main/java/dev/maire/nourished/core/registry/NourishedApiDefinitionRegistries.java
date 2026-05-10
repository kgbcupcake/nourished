package dev.maire.nourished.core.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.registry.AbsorptionModifierRegistry;
import dev.maire.nourished.api.registry.DietProfileRegistry;
import dev.maire.nourished.api.registry.MilestoneRegistry;
import dev.maire.nourished.api.registry.ReportProviderRegistry;
import dev.maire.nourished.api.registry.SeasonHookRegistry;
import dev.maire.nourished.api.registry.SynergyRegistry;

/**
 * Coordinates freeze/reset for API definition registries around bootstrap and datapack reload.
 */
@ApiStatus.Internal
public final class NourishedApiDefinitionRegistries {

    private static boolean datapackApplyCompletedOnce;

    private NourishedApiDefinitionRegistries() {}

    /**
     * Freezes list registries that only receive mod-constructor registrations (no datapack pass).
     */
    public static void freezeModOnlyRegistriesAfterCommonSetup() {
        AbsorptionModifierRegistry.freezeInternal();
        SeasonHookRegistry.freezeInternal();
        ReportProviderRegistry.freezeInternal();
    }

    /**
     * Called at the start of each {@link dev.maire.nourished.data.NourishedDataLoader} apply pass.
     * On the first pass, mod-constructor entries are preserved; on later passes, datapack-backed
     * registries are cleared before JSON is re-applied.
     */
    public static void onDatapackApplyBegin() {
        if (datapackApplyCompletedOnce) {
            DietProfileRegistry.resetInternal();
            MilestoneRegistry.resetInternal();
            SynergyRegistry.resetInternal();
        }
    }

    /**
     * Called at the end of each datapack apply pass, before the reload scope closes.
     */
    public static void onDatapackApplyEnd() {
        DietProfileRegistry.freezeInternal();
        MilestoneRegistry.freezeInternal();
        SynergyRegistry.freezeInternal();
        datapackApplyCompletedOnce = true;
    }
}
