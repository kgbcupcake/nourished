package dev.maire.nourished.client.config.categories.rawfood;

import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.core.RawFoodTierDef;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.buildSteppedFloatSlider;

public final class RawFoodConfigEntries {
    private RawFoodConfigEntries() {}
    public static void addRawFoodConfigEntries(List<AbstractConfigListEntry> entries, ConfigEntryBuilder eb) {
        if (entries == null) {
            return;
        }
        entries.add(rawFoodTierSubcategory(eb, RawSeverity.MILD, "mild", 600, -0.03f, 0.15f));
        entries.add(rawFoodTierSubcategory(eb, RawSeverity.MEDIUM, "medium", 1200, -0.05f, 0.35f));
        entries.add(rawFoodTierSubcategory(eb, RawSeverity.SEVERE, "severe", 1200, -0.08f, 0.60f));
        entries.add(
                eb.startIntSlider(Component.translatable("config.nourished.rawfood.memorySecs"), Math.round(RawFoodConfig.memorySecs() / 10.0f), 1, 60)
                        .setDefaultValue(12)
                        .setTextGetter(v -> Component.literal((v * 10) + " seconds"))
                        .setTooltip(Component.translatable("config.nourished.rawfood.memorySecs.desc"))
                        .setSaveConsumer(v -> RawFoodConfig.setMemorySecs(v * 10))
                        .build()
        );
    }

    public static AbstractConfigListEntry rawFoodTierSubcategory(
            ConfigEntryBuilder eb,
            RawSeverity severity,
            String keyPrefix,
            int defaultDurationTicks,
            float defaultNutrientPenalty,
            float defaultMissedOpportunity
    ) {
        RawFoodTierDef tier = RawFoodConfig.getTier(severity);
        List<AbstractConfigListEntry> tierEntries = new ArrayList<>();
        tierEntries.add(
                eb.startIntSlider(Component.translatable("config.nourished.rawfood." + keyPrefix + "Duration"), Math.round(tier.durationTicks() / 20.0f), 1, 300)
                        .setDefaultValue(defaultDurationTicks / 20)
                        .setTextGetter(v -> Component.literal((v * 20) + " ticks"))
                        .setTooltip(Component.translatable("config.nourished.rawfood." + keyPrefix + "Duration.desc"))
                        .setSaveConsumer(v -> RawFoodConfig.setDurationTicks(severity, v * 20))
                        .build()
        );
        tierEntries.add(buildSteppedFloatSlider(
                Component.translatable("config.nourished.rawfood." + keyPrefix + "NutrientPenalty"),
                tier.nutrientPenalty(),
                -0.5f,
                0.0f,
                0.01f,
                defaultNutrientPenalty,
                v -> RawFoodConfig.setNutrientPenalty(severity, v),
                () -> true,
                Component.translatable("config.nourished.rawfood." + keyPrefix + "NutrientPenalty.desc")
        ));
        tierEntries.add(buildSteppedFloatSlider(
                Component.translatable("config.nourished.rawfood." + keyPrefix + "MissedOpportunity"),
                tier.missedOpportunityMultiplier(),
                0.0f,
                1.0f,
                0.05f,
                defaultMissedOpportunity,
                v -> RawFoodConfig.setMissedOpportunityMultiplier(severity, v),
                () -> true,
                Component.translatable("config.nourished.rawfood." + keyPrefix + "MissedOpportunity.desc")
        ));
        return eb.startSubCategory(Component.literal(severity.name() + " Tier"), tierEntries)
                .setExpanded(false)
                .build();
    }
}
