package dev.maire.nourished.example;

import dev.maire.nourished.api.NourishedAPI;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

/**
 * Main example addon entrypoint showing end-to-end Nourished API usage from a third-party mod.
 */
@Mod(NourishedExampleAddon.MOD_ID)
public final class NourishedExampleAddon {

    public static final String MOD_ID = "nourished_example";

    public NourishedExampleAddon() {
        if (!ModList.get().isLoaded("nourished")) {
            return;
        }

        ExampleNutrientRegistration.register();
        ExampleFoodClassification.register();
        ExampleEffectRegistration.register();
        ExampleSynergyRegistration.register();
        ExampleMilestoneRegistration.register();
        ExampleDietProfile.register();
        ExampleCompatEntry.register();
        NourishedAPI.registerReportProvider(new ExampleReportProvider());
    }
}
