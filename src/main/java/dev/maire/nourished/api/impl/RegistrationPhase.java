package dev.maire.nourished.api.impl;

import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.event.NourishedRegisterEvent;
import dev.maire.nourished.api.impl.RegistrationBatch.Entry;
import dev.maire.nourished.api.impl.RegistrationBatch.FoodClassification;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.effects.SynergyDefinition;
import dev.marie.framework.api.effects.ThresholdEffect;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.progression.ProfileDefinition;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.compat.CompatDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/** Runs the one-time collect → validate → apply pass for {@link NourishedRegisterEvent}. */
@ApiStatus.Internal
public final class RegistrationPhase {

    public record Report(List<String> applied, List<String> rejected) {}

    private static boolean ran;
    private static Report lastReport = new Report(List.of(), List.of());

    private RegistrationPhase() {}

    public static Report lastReport() {
        return lastReport;
    }

    /** Returns how many nutrients the batch added. */
    public static int run() {
        if (ran) {
            return 0;
        }
        ran = true;
        RegistrationBatch batch = RegistrationBatch.open();
        NourishedRegisterEvent event = new NourishedRegisterEvent(batch);
        try {
            ModList.get().forEachModInOrder(mod -> {
                batch.setOwner(mod.getModId());
                mod.acceptEvent(event);
            });
        } finally {
            batch.close();
        }

        List<Entry> entries = batch.entries();
        RegistrationValidator.Result result = RegistrationValidator.validate(entries, NutrientRegistry.getKeys());
        List<String> errors = new ArrayList<>(result.errors());
        List<String> applied = new ArrayList<>();
        int nutrientsAdded = 0;
        for (Entry entry : result.accepted()) {
            try {
                apply(entry);
                applied.add("[" + entry.owner() + "] " + RegistrationValidator.describe(entry));
                if (entry.kind() == RegistrationBatch.Kind.NUTRIENT) {
                    nutrientsAdded++;
                }
            } catch (RuntimeException e) {
                errors.add("[" + entry.owner() + "] " + RegistrationValidator.describe(entry) + ": " + e.getMessage());
            }
        }
        lastReport = new Report(List.copyOf(applied), List.copyOf(errors));

        if (!entries.isEmpty()) {
            Nourished.LOGGER.info("[Nourished] NourishedRegisterEvent: applied {} of {} registrations ({} nutrients).",
                    entries.size() - errors.size(), entries.size(), nutrientsAdded);
        }
        if (!errors.isEmpty()) {
            Nourished.LOGGER.error("[Nourished] NourishedRegisterEvent rejected {} registration(s):", errors.size());
            errors.forEach(msg -> Nourished.LOGGER.error("[Nourished]   {}", msg));
        }
        return nutrientsAdded;
    }

    private static void apply(Entry entry) {
        Object def = entry.definition();
        switch (entry.kind()) {
            case NUTRIENT -> NourishedAPI.registerValue((ValueDefinition) def);
            case FOOD -> {
                FoodClassification food = (FoodClassification) def;
                NourishedAPI.registerSourceClassification(ResourceLocation.parse(food.sourceId()), food.valueKey(), food.amount());
            }
            case COMPAT -> NourishedAPI.registerCompatEntry((CompatDefinition) def);
            case EFFECT -> NourishedAPI.registerCustomEffect((ThresholdEffect) def);
            case NUTRIENT_SYNERGY -> NourishedAPI.registerValueSynergy((SynergyDefinition) def);
            case FOOD_SYNERGY -> NourishedAPI.registerSourcePairSynergy((SourcePairSynergy) def);
            case PROFILE -> NourishedAPI.registerTrackingProfile((ProfileDefinition) def);
            case MILESTONE -> NourishedAPI.registerMilestone((MilestoneDefinition) def);
        }
    }
}
