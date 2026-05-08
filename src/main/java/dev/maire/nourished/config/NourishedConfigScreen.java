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
import java.util.Comparator;
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
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(true);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        Map<String, PendingOverride> decayOverrides = new LinkedHashMap<>();
        Map<String, PendingOverride> criticalOverrides = new LinkedHashMap<>();
        Map<String, FoodValuePending> foodValuePending = new LinkedHashMap<>();
        Map<String, CompatPending> compatPending = new LinkedHashMap<>();
        Map<String, AtomicBoolean> modulePending = new LinkedHashMap<>();

        addPresetsCategory(builder, entryBuilder, parent);
        addModulesCategory(config, builder, entryBuilder, modulePending);
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
            for (Map.Entry<String, AtomicBoolean> entry : modulePending.entrySet()) {
                config.setModuleEnabled(entry.getKey(), entry.getValue().get());
            }
            // Dependency guard: critical toasts only makes sense when toasts are enabled.
            if (!config.isModuleEnabled("enableToasts")) {
                config.setModuleEnabled("enableCriticalToasts", false);
            }
            FoodValueRegistry.save();
            ColorRegistry.save();
            NutrientUiColors.clearOverrides();
            NourishedClientConfig.saveNow();
            NourishedConfig.saveNow();
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
            ConfigEntryBuilder eb,
            Map<String, AtomicBoolean> modulePending
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.modules"));
        List<ModuleMeta> metas = moduleMetas(config.moduleToggles());
        List<String> editableModuleKeys = new ArrayList<>();
        Map<String, List<AbstractConfigListEntry>> groupedEntries = new LinkedHashMap<>();
        for (String group : List.of("core", "ui", "other")) {
            groupedEntries.put(group, new ArrayList<>());
        }
        for (ModuleMeta meta : metas) {
            String key = meta.key;
            if (LockRegistry.isLocked(key)) {
                continue;
            }
            AtomicBoolean pending = new AtomicBoolean(config.isModuleEnabled(key));
            modulePending.put(key, pending);
            var entry = new ModuleToggleListEntry(
                    Component.translatable("config.nourished." + key),
                    Component.translatable("config.nourished." + key + ".desc"),
                    pending,
                    meta.group,
                    meta.dependsOn,
                    modulePending);
            boolean editable = !(LockRegistry.isServerOnly(key) && isMultiplayer());
            if (!editable) {
                entry.setEditable(false);
            } else {
                editableModuleKeys.add(key);
            }
            groupedEntries.getOrDefault(meta.group, groupedEntries.get("other")).add(entry);
        }

        if (!editableModuleKeys.isEmpty()) {
            category.addEntry(new ModuleProfilesListEntry(modulePending, editableModuleKeys));
            category.addEntry(new ModuleBulkToggleListEntry(editableModuleKeys, modulePending));
        }

        addModuleGroupSubcategory(category, eb, "core", groupedEntries.get("core"));
        addModuleGroupSubcategory(category, eb, "ui", groupedEntries.get("ui"));
        addModuleGroupSubcategory(category, eb, "other", groupedEntries.get("other"));

        if (!editableModuleKeys.isEmpty()) {
            category.addEntry(eb.startTextDescription(Component.translatable("config.nourished.modules.dependencyHint")).build());
        }

        addReloadButton(category, eb);
    }

    private static void addModuleGroupSubcategory(
            ConfigCategory category,
            ConfigEntryBuilder eb,
            String group,
            List<AbstractConfigListEntry> entries
    ) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        category.addEntry(
                eb.startSubCategory(groupTitle(group), entries)
                        .setExpanded(true)
                        .build()
        );
    }

    private static Component groupTitle(String group) {
        String icon = switch (group) {
            case "core" -> "⚙ ";
            case "ui" -> "🖵 ";
            default -> "▣ ";
        };
        return Component.literal(icon).append(Component.translatable("config.nourished.modules.group." + group));
    }

    private static List<ModuleMeta> moduleMetas(Map<String, ModConfigSpec.BooleanValue> moduleMap) {
        List<ModuleMeta> out = new ArrayList<>();
        for (String key : moduleMap.keySet()) {
            String group;
            String dependsOn = null;
            switch (key) {
                case "enableDecay", "enableEffects", "enableCalorieTracking", "enableSleepBonus" -> group = "core";
                case "enableHUD", "enableDietScreen", "enableFoodTooltips", "enableToasts", "enableCriticalToasts" -> group = "ui";
                default -> group = "other";
            }
            if ("enableCriticalToasts".equals(key)) {
                dependsOn = "enableToasts";
            }
            out.add(new ModuleMeta(key, group, dependsOn));
        }
        out.sort(Comparator.comparing((ModuleMeta m) -> m.group).thenComparing(m -> m.key));
        return out;
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

    private static final class ModuleBulkToggleListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_HEIGHT = 20;
        private static final int GAP = 6;

        private final List<String> editableModuleKeys;
        private final Map<String, AtomicBoolean> modulePending;
        private final Button enableAllButton;
        private final Button disableAllButton;

        ModuleBulkToggleListEntry(List<String> editableModuleKeys, Map<String, AtomicBoolean> modulePending) {
            super(
                    Component.translatable("config.nourished.modules.bulk"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.modules.bulk.desc")}),
                    false);
            this.editableModuleKeys = editableModuleKeys;
            this.modulePending = modulePending;
            this.enableAllButton = Button.builder(Component.translatable("config.nourished.modules.enableAll"), b -> setAll(true))
                    .bounds(0, 0, 120, BUTTON_HEIGHT)
                    .build();
            this.disableAllButton = Button.builder(Component.translatable("config.nourished.modules.disableAll"), b -> setAll(false))
                    .bounds(0, 0, 120, BUTTON_HEIGHT)
                    .build();
        }

        private void setAll(boolean value) {
            for (String key : editableModuleKeys) {
                AtomicBoolean pending = modulePending.get(key);
                if (pending != null) {
                    pending.set(value);
                }
            }
        }

        @Override
        public boolean isEdited() {
            return false;
        }

        @Override
        public void save() {
            // Module values are saved by each entry's save consumer.
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
            int btnWidth = (entryWidth - GAP) / 2;
            enableAllButton.active = isEditable();
            disableAllButton.active = isEditable();
            enableAllButton.setX(x);
            enableAllButton.setY(y);
            enableAllButton.setWidth(btnWidth);
            disableAllButton.setX(x + btnWidth + GAP);
            disableAllButton.setY(y);
            disableAllButton.setWidth(btnWidth);
            enableAllButton.render(graphics, mouseX, mouseY, delta);
            disableAllButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(enableAllButton, disableAllButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(enableAllButton, disableAllButton);
        }
    }

    private static final class ModuleProfilesListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_HEIGHT = 20;
        private static final int GAP = 6;

        private final Map<String, AtomicBoolean> modulePending;
        private final List<String> editableModuleKeys;
        private final Button balancedButton;
        private final Button minimalistButton;
        private final Button immersiveButton;

        ModuleProfilesListEntry(Map<String, AtomicBoolean> modulePending, List<String> editableModuleKeys) {
            super(
                    Component.translatable("config.nourished.modules.profiles"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.modules.profiles.desc")}),
                    false);
            this.modulePending = modulePending;
            this.editableModuleKeys = editableModuleKeys;
            this.balancedButton = Button.builder(Component.translatable("config.nourished.modules.profile.balanced"), b -> applyProfile("balanced"))
                    .bounds(0, 0, 110, BUTTON_HEIGHT)
                    .build();
            this.minimalistButton = Button.builder(Component.translatable("config.nourished.modules.profile.minimalist"), b -> applyProfile("minimalist"))
                    .bounds(0, 0, 110, BUTTON_HEIGHT)
                    .build();
            this.immersiveButton = Button.builder(Component.translatable("config.nourished.modules.profile.immersive"), b -> applyProfile("immersive"))
                    .bounds(0, 0, 110, BUTTON_HEIGHT)
                    .build();
        }

        private void applyProfile(String profile) {
            // Start with all OFF then enable profile-specific modules.
            for (String key : editableModuleKeys) {
                AtomicBoolean pending = modulePending.get(key);
                if (pending != null) pending.set(false);
            }
            switch (profile) {
                case "minimalist" -> setModules(true, "enableDecay", "enableEffects", "enableCalorieTracking");
                case "immersive" -> setModules(true, editableModuleKeys.toArray(new String[0]));
                default -> setModules(true,
                        "enableDecay",
                        "enableEffects",
                        "enableHUD",
                        "enableToasts",
                        "enableFoodTooltips",
                        "enableCalorieTracking",
                        "enableDietScreen",
                        "enableCriticalToasts",
                        "enableSleepBonus");
            }
            // Dependency guard.
            AtomicBoolean toasts = modulePending.get("enableToasts");
            AtomicBoolean crit = modulePending.get("enableCriticalToasts");
            if (toasts != null && crit != null && !toasts.get()) {
                crit.set(false);
            }
        }

        private void setModules(boolean value, String... keys) {
            for (String key : keys) {
                AtomicBoolean pending = modulePending.get(key);
                if (pending != null) pending.set(value);
            }
        }

        @Override
        public boolean isEdited() {
            return false;
        }

        @Override
        public void save() {}

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
            int btnWidth = (entryWidth - GAP * 2) / 3;
            balancedButton.active = isEditable();
            minimalistButton.active = isEditable();
            immersiveButton.active = isEditable();
            balancedButton.setX(x);
            balancedButton.setY(y);
            balancedButton.setWidth(btnWidth);
            minimalistButton.setX(x + btnWidth + GAP);
            minimalistButton.setY(y);
            minimalistButton.setWidth(btnWidth);
            immersiveButton.setX(x + (btnWidth + GAP) * 2);
            immersiveButton.setY(y);
            immersiveButton.setWidth(btnWidth);
            balancedButton.render(graphics, mouseX, mouseY, delta);
            minimalistButton.render(graphics, mouseX, mouseY, delta);
            immersiveButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(balancedButton, minimalistButton, immersiveButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(balancedButton, minimalistButton, immersiveButton);
        }
    }

    private static final class ModuleToggleListEntry extends TooltipListEntry<Boolean> {
        private static final int BUTTON_WIDTH = 130;
        private static final int BUTTON_HEIGHT = 20;
        private static final int COL_LABEL = 0xFFE0E0E0;
        private static final int COL_HINT = 0xFFCC8844;
        private static final int COL_ON_TEXT = 0xFFB8F2B8;
        private static final int COL_OFF_TEXT = 0xFFF0B2B2;
        private static final int COL_CHIP_ON = 0xFF2C7F2C;
        private static final int COL_CHIP_OFF = 0xFF8A2F2F;
        private static final int COL_CHIP_BORDER = 0xFF1A1A1A;
        private static final int COL_CHIP_HOVER = 0x22FFFFFF;
        private static final int COL_ROW_SEPARATOR = 0x223A3A3A;

        private final AtomicBoolean pending;
        private final String group;
        private final String dependsOnKey;
        private final Map<String, AtomicBoolean> modulePending;
        private final Button toggleButton;
        private final Component label;

        ModuleToggleListEntry(
                Component label,
                Component tooltip,
                AtomicBoolean pending,
                String group,
                String dependsOnKey,
                Map<String, AtomicBoolean> modulePending
        ) {
            super(label, () -> Optional.of(new Component[]{tooltip}), false);
            this.label = label;
            this.pending = pending;
            this.group = group;
            this.dependsOnKey = dependsOnKey;
            this.modulePending = modulePending;
            this.toggleButton = Button.builder(Component.empty(), b -> {
                        this.pending.set(!this.pending.get());
                        updateButtonLabel();
                    })
                    .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            updateButtonLabel();
        }

        private void updateButtonLabel() {
            toggleButton.setMessage(Component.literal(pending.get() ? "ON" : "OFF"));
        }

        @Override
        public boolean isEdited() {
            return false;
        }

        @Override
        public void save() {
            // Saved by outer screen save runnable.
        }

        @Override
        public Boolean getValue() {
            return pending.get();
        }

        @Override
        public Optional<Boolean> getDefaultValue() {
            return Optional.of(true);
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
            updateButtonLabel();
            toggleButton.active = isEditable();
            toggleButton.setX(x + entryWidth - BUTTON_WIDTH);
            toggleButton.setY(y);
            toggleButton.setWidth(BUTTON_WIDTH);
            String groupIcon = switch (group) {
                case "core" -> "⚙";
                case "ui" -> "🖵";
                default -> "▣";
            };
            graphics.drawString(Minecraft.getInstance().font, groupIcon, x, y + 6, 0xFF9AA5B1, false);
            graphics.drawString(Minecraft.getInstance().font, label, x + 12, y + 6, COL_LABEL, false);

            int chipX = x + 152;
            int chipY = y + 4;
            int chipW = 52;
            int chipH = 12;
            boolean isOn = pending.get();
            int chipFill = isOn ? COL_CHIP_ON : COL_CHIP_OFF;
            int chipText = isOn ? COL_ON_TEXT : COL_OFF_TEXT;
            graphics.fill(chipX, chipY, chipX + chipW, chipY + chipH, chipFill);
            graphics.renderOutline(chipX, chipY, chipW, chipH, COL_CHIP_BORDER);
            if (mouseX >= chipX && mouseX < chipX + chipW && mouseY >= chipY && mouseY < chipY + chipH) {
                graphics.fill(chipX, chipY, chipX + chipW, chipY + chipH, COL_CHIP_HOVER);
            }
            String chipLabel = isOn ? "ENABLED" : "DISABLED";
            int chipTextX = chipX + (chipW - Minecraft.getInstance().font.width(chipLabel)) / 2;
            graphics.drawString(Minecraft.getInstance().font, chipLabel, chipTextX, chipY + 2, chipText, false);
            if (dependsOnKey != null && pending.get()) {
                AtomicBoolean dep = modulePending.get(dependsOnKey);
                if (dep != null && !dep.get()) {
                    String depLabel = Component.translatable("config.nourished." + dependsOnKey).getString();
                    String depText = Component.translatable("config.nourished.modules.requires", depLabel).getString();
                    graphics.drawString(Minecraft.getInstance().font, depText, x + 152, y + 15, COL_HINT, false);
                }
            }
            graphics.fill(x, y + 23, x + entryWidth, y + 24, COL_ROW_SEPARATOR);
            toggleButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(toggleButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(toggleButton);
        }
    }

    private record ModuleMeta(String key, String group, String dependsOn) {}

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
