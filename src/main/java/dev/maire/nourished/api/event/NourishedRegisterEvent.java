package dev.maire.nourished.api.event;

import dev.maire.nourished.api.impl.RegistrationBatch;
import dev.maire.nourished.api.impl.RegistrationBatch.FoodClassification;
import dev.maire.nourished.api.impl.RegistrationBatch.Kind;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.effects.SynergyDefinition;
import dev.marie.framework.api.effects.ThresholdEffect;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.progression.ProfileDefinition;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.compat.CompatDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Fired once on each mod bus during common setup. Registrations are collected, validated as a batch
 * and applied nutrients-first, so listener order across addons never matters. See docs/API.md.
 */
@ApiStatus.Experimental
public final class NourishedRegisterEvent extends Event implements IModBusEvent {

    private final RegistrationBatch batch;

    @ApiStatus.Internal
    public NourishedRegisterEvent(RegistrationBatch batch) {
        this.batch = batch;
    }

    public void registerNutrient(ValueDefinition definition) {
        batch.submit(Kind.NUTRIENT, definition);
    }

    public void registerFood(ResourceLocation itemId, String nutrientKey, float amount) {
        batch.submit(Kind.FOOD, new FoodClassification(itemId == null ? null : itemId.toString(), nutrientKey, amount));
    }

    public void registerCompat(CompatDefinition definition) {
        batch.submit(Kind.COMPAT, definition);
    }

    public void registerEffect(ThresholdEffect definition) {
        batch.submit(Kind.EFFECT, definition);
    }

    public void registerNutrientSynergy(SynergyDefinition definition) {
        batch.submit(Kind.NUTRIENT_SYNERGY, definition);
    }

    public void registerFoodSynergy(SourcePairSynergy definition) {
        batch.submit(Kind.FOOD_SYNERGY, definition);
    }

    public void registerProfile(ProfileDefinition definition) {
        batch.submit(Kind.PROFILE, definition);
    }

    public void registerMilestone(MilestoneDefinition definition) {
        batch.submit(Kind.MILESTONE, definition);
    }
}
