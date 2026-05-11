package dev.maire.nourished.tooling.data;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.Set;

/**
 * Registers and exposes the Nourished datapack reload state.
 */
public final class NourishedDataManager {

    private static final NourishedDataLoader LOADER = new NourishedDataLoader();

    private NourishedDataManager() {}

    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(LOADER);
    }

    public static Set<ResourceLocation> getLoadedNutrients() {
        return LOADER.getLoadedNutrients();
    }

    public static Set<ResourceLocation> getLoadedFoodClassifications() {
        return LOADER.getLoadedFoodClassifications();
    }

    public static Set<ResourceLocation> getLoadedEffects() {
        return LOADER.getLoadedEffects();
    }

    public static Set<ResourceLocation> getLoadedSynergies() {
        return LOADER.getLoadedSynergies();
    }

    public static Set<ResourceLocation> getLoadedFoodSynergies() {
        return LOADER.getLoadedFoodSynergies();
    }

    public static Set<ResourceLocation> getLoadedMilestones() {
        return LOADER.getLoadedMilestones();
    }

    public static Set<ResourceLocation> getLoadedProfiles() {
        return LOADER.getLoadedProfiles();
    }

    public static Set<ResourceLocation> getLoadedCompatEntries() {
        return LOADER.getLoadedCompatEntries();
    }
}
