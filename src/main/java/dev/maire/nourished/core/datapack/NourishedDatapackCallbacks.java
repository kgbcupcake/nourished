package dev.maire.nourished.core.datapack;

import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.MariesLib.api.MarieAPI;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MilestoneDefinition;
import dev.marie.MariesLib.api.ThresholdEffect;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.data.MarieDataLoader;
import dev.marie.MariesLib.registry.MarieApiRegistries;
import dev.marie.MariesLib.runtime.SourceRegistry;
import net.minecraft.resources.ResourceLocation;

@ApiStatus.Internal
public final class NourishedDatapackCallbacks implements MarieDataLoader.Callbacks {

    @Override
    public void onApplyBegin() {
        SourceRegistry.clearExternalClassifications();
        MarieApiRegistries.onDatapackApplyBegin();
    }

    @Override
    public void onApplyEnd() {
        MarieApiRegistries.onDatapackApplyEnd();
    }

    @Override
    public void registerValue(ValueDefinition def) {
        MarieAPI.registerValue(def);
    }

    @Override
    public void registerSourceClassification(ResourceLocation itemId, String valueKey, float amount) {
        MarieAPI.registerSourceClassification(itemId, valueKey, amount);
    }

    @Override
    public void registerMilestone(MilestoneDefinition def) {
        MarieAPI.registerMilestone(def);
    }

    @Override
    public void registerCustomEffect(ThresholdEffect def) {
        EffectRegistry.upsertFromDatapack(def);
    }
}
