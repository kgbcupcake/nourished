package dev.maire.nourished.core.datapack;

import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.framework.api.MarieAPI;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MilestoneDefinition;
import dev.marie.framework.api.SourcePairSynergy;
import dev.marie.framework.api.ThresholdEffect;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.data.MarieDataLoader;
import dev.marie.framework.registry.MarieApiRegistries;
import dev.marie.framework.runtime.SourceRegistry;
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

    @Override
    public void registerSourcePairSynergy(SourcePairSynergy def) {
        MarieAPI.registerSourcePairSynergy(def);
    }
}
