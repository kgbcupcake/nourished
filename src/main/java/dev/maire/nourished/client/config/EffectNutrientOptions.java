package dev.maire.nourished.client.config;

import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.ArrayList;
import java.util.List;

/** Targets the effect editor's nutrient button cycles through: all, the food groups, then micro:<key>. */
final class EffectNutrientOptions {

    private EffectNutrientOptions() {}

    static List<String> forValue(String current) {
        List<String> opts = new ArrayList<>();
        opts.add("all");
        opts.addAll(NutrientRegistry.getKeys());
        for (MicroDef def : MicroRegistry.defs(true)) {
            opts.add(NutritionEffectApplier.MICRO_PREFIX + def.key());
        }
        // keep whatever the rule already targets so an unsynced or unknown key isn't lost on save
        if (current != null && !opts.contains(current)) {
            opts.add(current);
        }
        return opts;
    }
}
