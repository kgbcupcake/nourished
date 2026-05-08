package dev.maire.nourished.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.client.config.EffectBuilderWidget;
import dev.maire.nourished.client.config.FoodScannerWidget;
import dev.maire.nourished.client.config.ImportExportButtonsWidget;
import dev.maire.nourished.client.config.PresetsWidget;
import dev.maire.nourished.color.ColorRegistry;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.nutrition.FoodValueRegistry;
import dev.maire.nourished.nutrition.NutrientRegistry;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
        Map<String, FoodValuePending> foodValuePending = new LinkedHashMap<>();
        Map<String, CompatPending> compatPending = new LinkedHashMap<>();

        addPresetsCategory(builder, entryBuilder, parent);
        addModulesCategory(config, builder, entryBuilder);
        addGeneralCategory(config, builder, entryBuilder);
        addThresholdCategory(config, builder, entryBuilder);
        addEffectsCategory(config, builder, entryBuilder);
        addHudAndDisplayCategory(config, client, builder, entryBuilder);
        addNutrientsCategory(config, builder, entryBuilder, decayOverrides, criticalOverrides);
        addFoodValuesCategory(builder, entryBuilder, foodValuePending);
        addAdvancedCategory(config, builder, entryBuilder);
        addCompatibilityCategory(config, builder, entryBuilder, compatPending);

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
            for (Map.Entry<String, FoodValuePending> entry : foodValuePending.entrySet()) {
                FoodValuePending fv = entry.getValue();
                FoodValueRegistry.setCategory(
                        entry.getKey(),
                        fv.protein.get(),
                        fv.carbs.get(),
                        fv.fats.get(),
                        fv.vitamins.get(),
                        fv.hydration.get()
                );
            }
            for (Map.Entry<String, CompatPending> entry : compatPending.entrySet()) {
                String modid = entry.getKey();
                CompatPending cp = entry.getValue();
                ModConfigSpec.BooleanValue codeValue = config.compatCodeToggles().get(modid);
                ModConfigSpec.BooleanValue tagValue = config.compatTagToggles().get(modid);
                if (codeValue != null) codeValue.set(cp.codeCompat.get());
                if (tagValue != null) tagValue.set(cp.tagCompat.get());
            }
            FoodValueRegistry.save();
            ColorRegistry.save();
            NutrientUiColors.clearOverrides();
            NourishedClientConfig.saveNow();
        });
        return builder.build();
    }

    private static void addPresetsCategory(ConfigBuilder builder, ConfigEntryBuilder eb, Screen reopenParent) {
        PresetRegistry.ensureBuiltInFilesOnDisk();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.presets"));
        category.addEntry(new PresetsWidget(reopenParent));
        addReloadButton(category, eb);
        category.addEntry(new ImportExportButtonsWidget(reopenParent));
    }

    private static boolean isMultiplayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null;
    }

    private static void addModulesCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.modules"));

        if (!LockRegistry.isLocked("enableDecay")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableDecay"), config.enableDecay())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableDecay.desc"))
                    .setSaveConsumer(config::setEnableDecay)
                    .build();
            if (LockRegistry.isServerOnly("enableDecay") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableEffects")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableEffects"), config.enableEffects())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableEffects.desc"))
                    .setSaveConsumer(config::setEnableEffects)
                    .build();
            if (LockRegistry.isServerOnly("enableEffects") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableHUD")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableHUD"), config.enableHUD())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableHUD.desc"))
                    .setSaveConsumer(config::setEnableHUD)
                    .build();
            if (LockRegistry.isServerOnly("enableHUD") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableToasts")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableToasts"), config.enableToasts())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableToasts.desc"))
                    .setSaveConsumer(config::setEnableToasts)
                    .build();
            if (LockRegistry.isServerOnly("enableToasts") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableFoodTooltips")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableFoodTooltips"), config.enableFoodTooltips())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableFoodTooltips.desc"))
                    .setSaveConsumer(config::setEnableFoodTooltips)
                    .build();
            if (LockRegistry.isServerOnly("enableFoodTooltips") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableCalorieTracking")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableCalorieTracking"), config.enableCalorieTracking())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableCalorieTracking.desc"))
                    .setSaveConsumer(config::setEnableCalorieTracking)
                    .build();
            if (LockRegistry.isServerOnly("enableCalorieTracking") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableDietScreen")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableDietScreen"), config.enableDietScreen())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableDietScreen.desc"))
                    .setSaveConsumer(config::setEnableDietScreen)
                    .build();
            if (LockRegistry.isServerOnly("enableDietScreen") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableCriticalToasts")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableCriticalToasts"), config.enableCriticalToasts())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableCriticalToasts.desc"))
                    .setSaveConsumer(config::setEnableCriticalToasts)
                    .build();
            if (LockRegistry.isServerOnly("enableCriticalToasts") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("enableSleepBonus")) {
            var entry = eb.startBooleanToggle(Component.translatable("config.nourished.enableSleepBonus"), config.enableSleepBonus())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.nourished.enableSleepBonus.desc"))
                    .setSaveConsumer(config::setEnableSleepBonus)
                    .build();
            if (LockRegistry.isServerOnly("enableSleepBonus") && isMultiplayer()) {
                entry.setEditable(false);
            }
            category.addEntry(entry);
        }

        addReloadButton(category, eb);
    }

    private static void addGeneralCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.general"));

        if (!LockRegistry.isLocked("decayRate")) {
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
        }

        if (!LockRegistry.isLocked("decayIntervalTicks")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.decayIntervalTicks"), config.decayIntervalTicks(), 20, 72000)
                            .setDefaultValue(1200)
                            .setTextGetter(v -> Component.literal(v + " ticks"))
                            .setTooltip(Component.translatable("config.nourished.decayIntervalTicks.desc"))
                            .setSaveConsumer(config::setDecayIntervalTicks)
                            .build()
            );
        }

        if (!LockRegistry.isLocked("startingNutrientValue")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.startingNutrientValue"),
                            config.startingNutrientValue(),
                            0.0d,
                            1.0d,
                            0.5d,
                            config::setStartingNutrientValue,
                            Component.translatable("config.nourished.startingNutrientValue.desc")
                    )
            );
        }

        if (!LockRegistry.isLocked("nutrientGainScale")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrientGainScale"),
                            config.nutrientGainScale(),
                            0.5d,
                            20.0d,
                            5.0d,
                            config::setNutrientGainScale,
                            Component.translatable("config.nourished.nutrientGainScale.desc")
                    )
            );
        }

        if (!LockRegistry.isLocked("nutrientGainPerBiteMax")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrientGainPerBiteMax"),
                            config.nutrientGainPerBiteMax(),
                            0.05d,
                            1.0d,
                            0.2d,
                            config::setNutrientGainPerBiteMax,
                            Component.translatable("config.nourished.nutrientGainPerBiteMax.desc")
                    )
            );
        }

        addReloadButton(category, eb);
    }

    private static void addThresholdCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.thresholds"));

        if (!LockRegistry.isLocked("criticalThreshold")) {
            AbstractConfigListEntry<?> entry = buildDoubleSlider(
                    eb,
                    Component.translatable("config.nourished.criticalThreshold"),
                    config.criticalThreshold(),
                    0.0d,
                    1.0d,
                    0.25d,
                    config::setCriticalThreshold,
                    Component.translatable("config.nourished.criticalThreshold.desc")
            );
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("lowThreshold")) {
            AbstractConfigListEntry<?> entry = buildDoubleSlider(
                    eb,
                    Component.translatable("config.nourished.lowThreshold"),
                    config.lowThreshold(),
                    0.0d,
                    1.0d,
                    0.40d,
                    config::setLowThreshold,
                    Component.translatable("config.nourished.lowThreshold.desc")
            );
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("excessThreshold")) {
            AbstractConfigListEntry<?> entry = buildDoubleSlider(
                    eb,
                    Component.translatable("config.nourished.excessThreshold"),
                    config.excessThreshold(),
                    0.0d,
                    1.0d,
                    0.90d,
                    config::setExcessThreshold,
                    Component.translatable("config.nourished.excessThreshold.desc")
            );
            category.addEntry(entry);
        }

        if (!LockRegistry.isLocked("bonusEffectThreshold")) {
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
        }

        if (!LockRegistry.isLocked("penaltyEffectThreshold")) {
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

        addReloadButton(category, eb);
    }

    private static void addEffectsCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.effects"));

        if (!LockRegistry.isLocked("defaultEffectDurationTicks")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.defaultEffectDurationTicks"), config.defaultEffectDurationTicks(), 20, 72000)
                            .setDefaultValue(140)
                            .setTextGetter(v -> Component.literal(v + " ticks"))
                            .setTooltip(Component.translatable("config.nourished.defaultEffectDurationTicks.desc"))
                            .setSaveConsumer(config::setDefaultEffectDurationTicks)
                            .build()
            );
        }

        category.addEntry(new EffectBuilderWidget());

        addReloadButton(category, eb);
    }

    private static void addHudAndDisplayCategory(NourishedConfig config, NourishedClientConfig client, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.hud_display"));

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

        addReloadButton(category, eb);
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

        addReloadButton(nutrients, eb);
    }

    private static void addFoodValuesCategory(ConfigBuilder builder, ConfigEntryBuilder eb, Map<String, FoodValuePending> foodValuePending) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.food_values"));

        for (FoodValueRegistry.FoodValueDef def : FoodValueRegistry.getAll()) {
            String categoryKey = def.category();
            FoodValuePending pending = new FoodValuePending(
                    def.protein(), def.carbs(), def.fats(), def.vitamins(), def.hydration()
            );
            foodValuePending.put(categoryKey, pending);

            List<AbstractConfigListEntry> entries = new ArrayList<>();
            entries.add(buildFloatSlider(eb, Component.literal("Protein"), pending.protein.get(), 0f, 2f, def.protein(), pending.protein::set));
            entries.add(buildFloatSlider(eb, Component.literal("Carbs"), pending.carbs.get(), 0f, 2f, def.carbs(), pending.carbs::set));
            entries.add(buildFloatSlider(eb, Component.literal("Fats"), pending.fats.get(), 0f, 2f, def.fats(), pending.fats::set));
            entries.add(buildFloatSlider(eb, Component.literal("Vitamins"), pending.vitamins.get(), 0f, 2f, def.vitamins(), pending.vitamins::set));
            entries.add(buildFloatSlider(eb, Component.literal("Hydration"), pending.hydration.get(), 0f, 2f, def.hydration(), pending.hydration::set));

            category.addEntry(
                    eb.startSubCategory(Component.literal(prettyKey(categoryKey)), entries)
                            .setExpanded(false)
                            .build()
            );
        }

        addReloadButton(category, eb);
    }

    private static void addAdvancedCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.advanced"));

        if (!LockRegistry.isLocked("calorieDisplayMax")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.calorieDisplayMax"), config.calorieDisplayMax(), 100, 100000)
                            .setDefaultValue(2000)
                            .setTextGetter(v -> Component.literal(String.valueOf(v)))
                            .setTooltip(Component.translatable("config.nourished.calorieDisplayMax.desc"))
                            .setSaveConsumer(config::setCalorieDisplayMax)
                            .build()
            );
        }

        category.addEntry(new FoodScannerWidget());

        addReloadButton(category, eb);
    }

    private static void addCompatibilityCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, CompatPending> compatPending
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.compatibility"));

        for (String modid : ModCompat.getDetected().keySet()) {
            boolean detected = ModCompat.getDetected().getOrDefault(modid, false);
            String modName = toTitleCase(modid);
            String statusText = detected ? "Status: Installed ✓" : "Status: Not detected ✗";

            CompatPending pending = new CompatPending(
                    config.isCodeCompatEnabled(modid),
                    config.isTagCompatEnabled(modid)
            );
            compatPending.put(modid, pending);

            List<AbstractConfigListEntry> entries = new ArrayList<>();

            String codeKey = "compat." + modid + ".enableCodeCompat";
            if (!LockRegistry.isLocked(codeKey)) {
                var codeEntry = eb.startBooleanToggle(
                                Component.translatable("config.nourished.compat.enableCodeCompat"),
                                pending.codeCompat.get()
                        )
                        .setDefaultValue(true)
                        .setTooltip(
                                Component.literal(statusText),
                                Component.translatable("config.nourished.compat.enableCodeCompat.desc")
                        )
                        .setSaveConsumer(pending.codeCompat::set)
                        .build();
                if (LockRegistry.isServerOnly(codeKey) && isMultiplayer()) {
                    codeEntry.setEditable(false);
                }
                entries.add(codeEntry);
            }

            String tagKey = "compat." + modid + ".enableTagCompat";
            if (!LockRegistry.isLocked(tagKey)) {
                var tagEntry = eb.startBooleanToggle(
                                Component.translatable("config.nourished.compat.enableTagCompat"),
                                pending.tagCompat.get()
                        )
                        .setDefaultValue(true)
                        .setTooltip(
                                Component.literal(statusText),
                                Component.translatable("config.nourished.compat.enableTagCompat.desc")
                        )
                        .setSaveConsumer(pending.tagCompat::set)
                        .build();
                if (LockRegistry.isServerOnly(tagKey) && isMultiplayer()) {
                    tagEntry.setEditable(false);
                }
                entries.add(tagEntry);
            }

            if (!entries.isEmpty()) {
                category.addEntry(
                        eb.startSubCategory(Component.literal(modName), entries)
                                .setExpanded(false)
                                .build()
                );
            }
        }

        addReloadButton(category, eb);
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            } else {
                result.append(' ');
                capitalizeNext = true;
            }
        }
        return result.toString().trim();
    }

    private static void addReloadButton(ConfigCategory category, ConfigEntryBuilder eb) {
        category.addEntry(
                eb.startTextDescription(Component.empty()).build()
        );
        category.addEntry(new ReloadConfigsListEntry());
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

    private static AbstractConfigListEntry buildFloatSlider(
            ConfigEntryBuilder eb,
            Component label,
            float value,
            float min,
            float max,
            float defaultValue,
            java.util.function.Consumer<Float> saveConsumer,
            Component... tooltip
    ) {
        int scale = 1000;
        int minScaled = Math.round(min * scale);
        int maxScaled = Math.round(max * scale);
        int valueScaled = Math.round(Math.max(min, Math.min(max, value)) * scale);
        int defaultScaled = Math.round(Math.max(min, Math.min(max, defaultValue)) * scale);
        return eb.startIntSlider(label, valueScaled, minScaled, maxScaled)
                .setDefaultValue(defaultScaled)
                .setTextGetter(v -> Component.literal(String.format(Locale.ROOT, "%.3f", v / (float) scale)))
                .setTooltip(tooltip)
                .setSaveConsumer(v -> saveConsumer.accept(v / (float) scale))
                .build();
    }

    /** Captures Cloth entries so the live HUD preview can read pending slider values before save. */
    private static final class PreviewRefs {
        private static final int SCALE = 1000;

        private final NourishedConfig config;
        AbstractConfigListEntry<Integer> criticalSlider;
        AbstractConfigListEntry<Integer> lowSlider;
        AbstractConfigListEntry<Integer> excessSlider;
        AbstractConfigListEntry<Boolean> enableHudToggle;

        private PreviewRefs(NourishedConfig config) {
            this.config = config;
        }

        double criticalAsDouble() {
            return criticalSlider != null ? criticalSlider.getValue() / (double) SCALE : config.criticalThreshold();
        }

        double lowAsDouble() {
            return lowSlider != null ? lowSlider.getValue() / (double) SCALE : config.lowThreshold();
        }

        double excessAsDouble() {
            return excessSlider != null ? excessSlider.getValue() / (double) SCALE : config.excessThreshold();
        }

        boolean hudEnabledAsBoolean() {
            return enableHudToggle != null ? Boolean.TRUE.equals(enableHudToggle.getValue()) : config.enableHUD();
        }
    }

    @SuppressWarnings("unchecked")
    private static AbstractConfigListEntry<Integer> castIntEntry(AbstractConfigListEntry<?> entry) {
        return (AbstractConfigListEntry<Integer>) entry;
    }

    @SuppressWarnings("unchecked")
    private static AbstractConfigListEntry<Boolean> castBoolEntry(AbstractConfigListEntry<?> entry) {
        return (AbstractConfigListEntry<Boolean>) entry;
    }

    private static final class PendingOverride {
        private final AtomicBoolean enabled;
        private final AtomicReference<Double> value;

        private PendingOverride(boolean enabled, double value) {
            this.enabled = new AtomicBoolean(enabled);
            this.value = new AtomicReference<>(value);
        }
    }

    private static final class FoodValuePending {
        private final AtomicReference<Float> protein;
        private final AtomicReference<Float> carbs;
        private final AtomicReference<Float> fats;
        private final AtomicReference<Float> vitamins;
        private final AtomicReference<Float> hydration;

        private FoodValuePending(float protein, float carbs, float fats, float vitamins, float hydration) {
            this.protein = new AtomicReference<>(protein);
            this.carbs = new AtomicReference<>(carbs);
            this.fats = new AtomicReference<>(fats);
            this.vitamins = new AtomicReference<>(vitamins);
            this.hydration = new AtomicReference<>(hydration);
        }
    }

    private static final class CompatPending {
        private final AtomicBoolean codeCompat;
        private final AtomicBoolean tagCompat;

        private CompatPending(boolean codeCompat, boolean tagCompat) {
            this.codeCompat = new AtomicBoolean(codeCompat);
            this.tagCompat = new AtomicBoolean(tagCompat);
        }
    }

    /**
     * Cloth Config 15 has no {@code startButton}; this mirrors {@code BooleanListEntry} layout with a single action
     * button (no save consumer — reload runs immediately on click).
     */
    private static final class ReloadConfigsListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_WIDTH = 150;
        private static final int BUTTON_HEIGHT = 20;

        private final Button button;

        ReloadConfigsListEntry() {
            super(
                    Component.translatable("config.nourished.reloadConfigs"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.reloadConfigs.desc")}),
                    false);
            this.button = Button.builder(Component.translatable("config.nourished.reloadConfigs"), b -> {
                        NutrientRegistry.reload();
                        EffectRegistry.reload();
                        PresetRegistry.reload();
                        ColorRegistry.reload();
                        FoodValueRegistry.reload();
                        FoodOverrideRegistry.reload();
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(
                                    Component.translatable("config.nourished.reloadConfigs.chat"), false);
                        }
                    })
                    .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
        }

        @Override
        public boolean isEdited() {
            return false;
        }

        @Override
        public void save() {
            // Instant action on click; nothing to persist when the config screen saves.
        }

        @Override
        public Object getValue() {
            return Boolean.FALSE;
        }

        @Override
        public Optional<Object> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public int getItemHeight() {
            return 24;
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int y,
                int x,
                int entryWidth,
                int entryHeight,
                int mouseX,
                int mouseY,
                boolean isHovered,
                float delta) {
            button.active = isEditable();
            button.setY(y);
            button.setX(x + entryWidth - BUTTON_WIDTH);
            button.setWidth(BUTTON_WIDTH);
            button.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(button);
        }
    }
}
