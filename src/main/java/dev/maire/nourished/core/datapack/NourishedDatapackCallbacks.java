package dev.maire.nourished.core.datapack;

import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.progression.TrackerMilestoneDefinition;
import dev.marie.framework.api.registry.TrackerMilestoneRegistry;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.effects.ThresholdEffect;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.compat.CompatDefinition;
import dev.marie.framework.compat.ModCompat;
import dev.marie.framework.data.MarieDataLoader;
import dev.marie.framework.registry.MarieApiRegistries;
import dev.marie.framework.runtime.SourceRegistry;
import dev.maire.nourished.core.nutrition.FoodOverrideRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

@ApiStatus.Internal
public final class NourishedDatapackCallbacks implements MarieDataLoader.Callbacks {

    @Override
    public void onApplyBegin() {
        SourceRegistry.clearExternalClassifications();
        FoodOverrideRegistry.beginDatapackReplace();
        MarieApiRegistries.onDatapackApplyBegin();
    }

    @Override
    public void onApplyEnd() {
        MarieApiRegistries.onDatapackApplyEnd();
        FoodOverrideRegistry.endDatapackReplace();
    }

    @Override
    public void registerFoodOverride(ResourceLocation itemId, Map<String, Float> nutrients, int calories, boolean enabled) {
        FoodOverrideRegistry.acceptDatapackOverride(itemId.toString(), nutrients, calories, enabled);
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
    public void registerTrackerMilestone(TrackerMilestoneDefinition def) {
        TrackerMilestoneRegistry.register(def);
    }

    @Override
    public void registerCustomEffect(ThresholdEffect def) {
        EffectRegistry.upsertFromDatapack(def);
    }

    @Override
    public void registerSourcePairSynergy(SourcePairSynergy def) {
        MarieAPI.registerSourcePairSynergy(def);
    }

    @Override
    public void registerCompatEntry(CompatDefinition def) {
        ModCompat.registerExternal(def);
    }
}
