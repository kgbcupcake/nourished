package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ThresholdEffect;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.core.MarieLibRegistrationDelegate;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.framework.runtime.SourceRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

@ApiStatus.Internal
final class NourishedRegistrationDelegate implements MarieLibRegistrationDelegate {

    @Override
    public List<String> getValueKeys() {
        return NutrientRegistry.getKeys();
    }

    @Override
    public void registerValue(ValueDefinition definition) {
        NutrientRegistry.registerExternal(definition);
    }

    @Override
    public void registerEffect(ThresholdEffect definition) {
        EffectRegistry.registerExternal(definition);
    }

    @Override
    public void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount) {
        SourceRegistry.registerClassification(sourceId, valueKey, amount);
    }
}
