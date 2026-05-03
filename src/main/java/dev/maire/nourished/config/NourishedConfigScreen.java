package dev.maire.nourished.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maire.nourished.client.NourishedKeys;
import org.lwjgl.glfw.GLFW;
import dev.maire.nourished.nutrition.NutrientRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleConsumer;

public final class NourishedConfigScreen {

    private NourishedConfigScreen() {}

    public static Screen create(Screen parent) {
        NourishedConfig config = NourishedConfig.get();
        NourishedClientConfig client = NourishedClientConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.nourished.title"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        Map<String, PendingOverride> decayOverrides = new LinkedHashMap<>();
        Map<String, PendingOverride> criticalOverrides = new LinkedHashMap<>();

        addGeneralCategory(config, builder, entryBuilder);
        addGuiTweaksCategory(config, client, builder, entryBuilder);
        addThresholdCategory(config, builder, entryBuilder);
        addEffectsCategory(config, builder, entryBuilder);
        addNutrientsCategory(config, builder, entryBuilder, decayOverrides, criticalOverrides);

        builder.setSavingRunnable(() -> {
            for (Map.Entry<String, PendingOverride> entry : decayOverrides.entrySet()) {
                ModConfigSpec.DoubleValue value = config.nutrientDecayRateOverrides().get(entry.getKey());
                if (value == null) continue;
                value.set(entry.getValue().enabled.get() ? entry.getValue().value.get() : -1.0d);
            }
            for (Map.Entry<String, PendingOverride> entry : criticalOverrides.entrySet()) {
                ModConfigSpec.DoubleValue value = config.nutrientCriticalThresholdOverrides().get(entry.getKey());
                if (value == null) continue;
                value.set(entry.getValue().enabled.get() ? entry.getValue().value.get() : -1.0d);
            }
            NourishedClientConfig.saveNow();
        });
        return builder.build();
    }

    private static void addGeneralCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.general"));
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.decayRate"),
                        config.decayRate(),
                        0.0d,
                        1.0d,
                        0.1d,
                        config::setDecayRate,
                        Component.translatable("config.nourished.decayRate.desc")
                )
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.decayIntervalTicks"), config.decayIntervalTicks(), 20, 72000)
                        .setDefaultValue(1200)
                        .setTextGetter(v -> Component.literal(v + " ticks"))
                        .setTooltip(Component.translatable("config.nourished.decayIntervalTicks.desc"))
                        .setSaveConsumer(config::setDecayIntervalTicks)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.enableEffects"), config.enableEffects())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.enableEffects.desc"))
                        .setSaveConsumer(config::setEnableEffects)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showFoodTooltips"), config.showFoodTooltips())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.showFoodTooltips.desc"))
                        .setSaveConsumer(config::setShowFoodTooltips)
                        .build()
        );
    }

    private static void addGuiTweaksCategory(NourishedConfig config, NourishedClientConfig client, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.gui_tweaks"));
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.enableHUD"), config.enableHUD())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.enableHUD.desc"))
                        .setSaveConsumer(config::setEnableHUD)
                        .build()
        );
        category.addEntry(
                eb.startEnumSelector(Component.translatable("config.nourished.hudAnchor"), HudAnchor.class, client.hudAnchor())
                        .setDefaultValue(HudAnchor.BOTTOM_LEFT)
                        .setTooltip(Component.translatable("config.nourished.hudAnchor.desc"))
                        .setSaveConsumer(client::setHudAnchor)
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudOffsetX"), client.hudOffsetX(), -2000, 2000)
                        .setDefaultValue(0)
                        .setTooltip(Component.translatable("config.nourished.hudOffsetX.desc"))
                        .setSaveConsumer(client::setHudOffsetX)
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudOffsetY"), client.hudOffsetY(), -2000, 2000)
                        .setDefaultValue(0)
                        .setTooltip(Component.translatable("config.nourished.hudOffsetY.desc"))
                        .setSaveConsumer(client::setHudOffsetY)
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudBarWidth"), client.hudBarWidth(), 40, 120)
                        .setDefaultValue(60)
                        .setTooltip(Component.translatable("config.nourished.hudBarWidth.desc"))
                        .setSaveConsumer(client::setHudBarWidth)
                        .build()
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.hudScale"),
                        client.hudScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setHudScale,
                        Component.translatable("config.nourished.hudScale.desc")
                )
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudReservedBottom"), client.hudReservedBottom(), 30, 100)
                        .setDefaultValue(52)
                        .setTooltip(Component.translatable("config.nourished.hudReservedBottom.desc"))
                        .setSaveConsumer(client::setHudReservedBottom)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.hudDraggable"), client.hudDraggable())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.hudDraggable.desc"))
                        .setSaveConsumer(client::setHudDraggable)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.dietBarDragEnabled"), client.dietBarDragEnabled())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.dietBarDragEnabled.desc"))
                        .setSaveConsumer(client::setDietBarDragEnabled)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.hideZeroNutrients"), client.hideZeroNutrients())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.hideZeroNutrients.desc"))
                        .setSaveConsumer(client::setHideZeroNutrients)
                        .build()
        );
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.hudEditHotkey"),
                                NourishedKeys.EDIT_HUD.getKey()
                        )
                        .setDefaultValue(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H))
                        .setTooltip(Component.translatable("config.nourished.hudEditHotkey.desc"))
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.EDIT_HUD.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );
    }

    private static void addThresholdCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.thresholds"));
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.criticalThreshold"),
                        config.criticalThreshold(),
                        0.0d,
                        1.0d,
                        0.25d,
                        config::setCriticalThreshold,
                        Component.translatable("config.nourished.criticalThreshold.desc")
                )
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.lowThreshold"),
                        config.lowThreshold(),
                        0.0d,
                        1.0d,
                        0.40d,
                        config::setLowThreshold,
                        Component.translatable("config.nourished.lowThreshold.desc")
                )
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.excessThreshold"),
                        config.excessThreshold(),
                        0.0d,
                        1.0d,
                        0.90d,
                        config::setExcessThreshold,
                        Component.translatable("config.nourished.excessThreshold.desc")
                )
        );
    }

    private static void addEffectsCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.effects"));
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.enableEffects"), config.enableEffects())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.nourished.enableEffects.desc"))
                        .setSaveConsumer(config::setEnableEffects)
                        .build()
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.bonusEffectThreshold"),
                        config.bonusEffectThreshold(),
                        0.0d,
                        1.0d,
                        0.75d,
                        config::setBonusEffectThreshold,
                        Component.translatable("config.nourished.bonusEffectThreshold.desc")
                )
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.penaltyEffectThreshold"),
                        config.penaltyEffectThreshold(),
                        0.0d,
                        1.0d,
                        0.25d,
                        config::setPenaltyEffectThreshold,
                        Component.translatable("config.nourished.penaltyEffectThreshold.desc")
                )
        );
    }

    private static void addNutrientsCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, PendingOverride> decayOverrides,
            Map<String, PendingOverride> criticalOverrides
    ) {
        ConfigCategory nutrients = builder.getOrCreateCategory(Component.translatable("config.nourished.category.nutrients"));
        for (String key : NutrientRegistry.getKeys()) {
            double decayRaw = config.nutrientDecayRateOverrides().get(key).get();
            double criticalRaw = config.nutrientCriticalThresholdOverrides().get(key).get();

            PendingOverride decay = new PendingOverride(decayRaw >= 0d, decayRaw >= 0d ? decayRaw : config.decayRate());
            PendingOverride critical = new PendingOverride(criticalRaw >= 0d, criticalRaw >= 0d ? criticalRaw : config.criticalThreshold());
            decayOverrides.put(key, decay);
            criticalOverrides.put(key, critical);

            List<AbstractConfigListEntry> entries = new ArrayList<>();
            entries.add(
                    eb.startBooleanToggle(Component.translatable("config.nourished.nutrient.overrideDecay", prettyKey(key)), decay.enabled.get())
                            .setDefaultValue(false)
                            .setSaveConsumer(decay.enabled::set)
                            .build()
            );
            entries.add(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrient.decayRate", prettyKey(key)),
                            decay.value.get(),
                            0.0d,
                            1.0d,
                            config.decayRate(),
                            decay.value::set
                    )
            );
            entries.add(
                    eb.startBooleanToggle(Component.translatable("config.nourished.nutrient.overrideCritical", prettyKey(key)), critical.enabled.get())
                            .setDefaultValue(false)
                            .setSaveConsumer(critical.enabled::set)
                            .build()
            );
            entries.add(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrient.criticalThreshold", prettyKey(key)),
                            critical.value.get(),
                            0.0d,
                            1.0d,
                            config.criticalThreshold(),
                            critical.value::set
                    )
            );

            nutrients.addEntry(
                    eb.startSubCategory(Component.translatable("config.nourished.nutrient.category", prettyKey(key)), entries)
                            .setExpanded(false)
                            .build()
            );
        }
    }

    private static String prettyKey(String key) {
        return Component.translatable("nourished.screen.diet.bar." + key).getString();
    }

    private static AbstractConfigListEntry buildDoubleSlider(
            ConfigEntryBuilder eb,
            Component label,
            double value,
            double min,
            double max,
            double defaultValue,
            DoubleConsumer saveConsumer,
            Component... tooltip
    ) {
        int scale = 1000;
        int minScaled = (int) Math.round(min * scale);
        int maxScaled = (int) Math.round(max * scale);
        int valueScaled = (int) Math.round(Math.max(min, Math.min(max, value)) * scale);
        int defaultScaled = (int) Math.round(Math.max(min, Math.min(max, defaultValue)) * scale);
        return eb.startIntSlider(label, valueScaled, minScaled, maxScaled)
                .setDefaultValue(defaultScaled)
                .setTextGetter(v -> Component.literal(String.format(Locale.ROOT, "%.3f", v / (double) scale)))
                .setTooltip(tooltip)
                .setSaveConsumer(v -> saveConsumer.accept(v / (double) scale))
                .build();
    }

    private static final class PendingOverride {
        private final AtomicBoolean enabled;
        private final AtomicReference<Double> value;

        private PendingOverride(boolean enabled, double value) {
            this.enabled = new AtomicBoolean(enabled);
            this.value = new AtomicReference<>(value);
        }
    }
}
