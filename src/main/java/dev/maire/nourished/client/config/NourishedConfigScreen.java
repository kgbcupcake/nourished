package dev.maire.nourished.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.client.config.EffectBuilderWidget;
import dev.maire.nourished.client.config.FoodScannerWidget;
import dev.maire.nourished.client.config.ImportExportButtonsWidget;
import dev.maire.nourished.client.config.PresetsWidget;
import dev.maire.nourished.compat.CompatEntry;
import dev.maire.nourished.compat.CompatReportEntry;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.core.color.ColorRegistry;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import dev.maire.nourished.core.reload.NourishedReloadPipeline;
import dev.maire.nourished.config.HudAnchor;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.PresetRegistry;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

import net.minecraft.Util;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        addScannerCategory(config, builder, entryBuilder);
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
            AtomicBoolean toastsPending = modulePending.get("enableToasts");
            if (toastsPending != null && !toastsPending.get()) {
                config.setModuleEnabled("enableCriticalToasts", false);
            }
            FoodValueRegistry.save();
            ColorRegistry.save();
            EffectRegistry.save();
            NutrientUiColors.clearOverrides();
            NourishedClientConfig.saveNow();
            NourishedConfig.saveNow();
            ModuleCache.refresh();
        });
        builder.setAlwaysShowTabs(true);
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
                    moduleToggleTitle(key),
                    moduleToggleDescription(key),
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
            category.addEntry(new StyledChipTextEntry(Component.translatable("config.nourished.modules.dependencyHint"), 0xFFCC8844));
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
        return Component.translatable("config.nourished.modules.group." + group);
    }

    private static Component moduleToggleTitle(String key) {
        return Component.translatable(switch (key) {
            case "blockHeavyMeals" -> "nourished.config.blockHeavyMeals";
            case "blockLightFood" -> "nourished.config.blockLightFood";
            default -> "config.nourished." + key;
        });
    }

    private static Component moduleToggleDescription(String key) {
        return Component.translatable(switch (key) {
            case "blockHeavyMeals" -> "nourished.config.blockHeavyMeals.desc";
            case "blockLightFood" -> "nourished.config.blockLightFood.desc";
            default -> "config.nourished." + key + ".desc";
        });
    }

    private static List<ModuleMeta> moduleMetas(Map<String, ModConfigSpec.BooleanValue> moduleMap) {
        List<ModuleMeta> out = new ArrayList<>();
        for (String key : moduleMap.keySet()) {
            String group;
            String dependsOn = null;
            switch (key) {
                case "enableDecay", "enableNutritionEating", "blockHeavyMeals", "blockLightFood",
                     "enableEffects", "enableCalorieTracking", "enableSleepBonus",
                     "enableSynergies", "enableMilestones", "enableSeasonHooks", "enableAbsorptionModifiers" -> group = "core";
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
        category.addEntry(new HudQuickActionsListEntry(client));

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

        addReloadButton(category, eb);
    }

    private static void addScannerCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.scanner"));

        if (!LockRegistry.isLocked("scanner.enableRecipeInheritance")) {
            category.addEntry(
                    eb.startBooleanToggle(
                                    Component.translatable("config.nourished.scanner.enableRecipeInheritance"),
                                    config.scannerEnableRecipeInheritance()
                            )
                            .setDefaultValue(true)
                            .setTooltip(Component.translatable("config.nourished.scanner.enableRecipeInheritance.desc"))
                            .setSaveConsumer(config::setScannerEnableRecipeInheritance)
                            .build()
            );
        }

        if (!LockRegistry.isLocked("scanner.confidenceSpreadThreshold")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.scanner.confidenceSpreadThreshold"),
                            config.scannerConfidenceSpreadThreshold(),
                            0.0d,
                            20.0d,
                            3.0d,
                            config::setScannerConfidenceSpreadThreshold,
                            Component.translatable("config.nourished.scanner.confidenceSpreadThreshold.desc")
                    )
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
        for (String modid : config.compatCodeToggles().keySet()) {
            compatPending.put(modid, new CompatPending(
                    config.isCodeCompatEnabled(modid),
                    config.isTagCompatEnabled(modid)
            ));
        }
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.compat"));
        category.addEntry(new CompatTabbedListEntry(config, compatPending));
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

    private static final class CompatTabbedListEntry extends TooltipListEntry<Object> {
        private static final int TAB_BAR_H = 24;
        private static final int ROW_H = 24;
        private static final int BUILTIN_PANEL_H = 36;
        private static final int VIEWPORT_H = 240;
        private static final int DETECTED_TOOLBAR_H = 42;
        private static final int GAP = 6;
        private static final int COL_BG = 0x66000000;
        private static final int COL_ROW_SEPARATOR = 0x223A3A3A;
        private static final int COL_TEXT = 0xFFE0E0E0;
        private static final int COL_SUBTEXT = 0xFFAAAAAA;
        private static final int COL_TAB_BG = 0xAA1C1C1C;
        private static final int COL_TAB_ACTIVE = 0xFF2E5C7F;
        private static final int COL_TAB_BORDER_ACTIVE = 0xFF5DA9DE;
        private static final int COL_TAB_BORDER_INACTIVE = 0xFF4A4A4A;
        private static final int COL_CHIP_GREEN = 0xFF2C7F2C;
        private static final int COL_CHIP_RED = 0xFF8A2F2F;
        private static final int COL_CHIP_YELLOW = 0xFF9C7A18;
        private static final int COL_CHIP_BORDER = 0xFF1A1A1A;

        private final Map<String, CompatPending> compatPending;
        private final List<CompatReportEntry> detectedRows;
        private final List<CompatEntry> builtInRows;
        private final List<String> settingsRows;

        private final Button detectedTabButton;
        private final Button builtInTabButton;
        private final Button settingsTabButton;
        private final String detectedTabLabel;
        private final String builtInTabLabel;
        private final String settingsTabLabel;
        private final EditBox detectedSearchBox;
        private final Button detectedSortNameButton;
        private final Button detectedSortStatusButton;
        private final Button detectedSortCategoryButton;

        private final Map<String, Button> codeButtons = new LinkedHashMap<>();
        private final Map<String, Button> tagButtons = new LinkedHashMap<>();
        private final Button builtInCodeButton;
        private final Button builtInTagButton;
        private final Button openDatapackFolderButton;
        private final Map<String, CompatEntry> builtInByModId = new LinkedHashMap<>();
        private final Map<String, int[]> detectedFoodCounts = new LinkedHashMap<>();
        private final Map<String, ResourceLocation> modLogoCache = new LinkedHashMap<>();

        private int tabIndex;
        private int scrollOffset;
        private int expandedBuiltInIndex = -1;
        private String expandedBuiltInModId;
        private long flashUntilMs;
        private String flashedModId;
        private int hoveredDetectedIndex = -1;
        private SortKey detectedSortKey = SortKey.DEFAULT;
        private boolean detectedNameDesc;
        private boolean detectedStatusMissingFirst;
        private boolean detectedCategoryDesc;
        private String lastSearchText = "";
        private boolean detectedFoodCountsComputed;
        private int listX;
        private int listY;
        private int listW;
        private int listH;
        private int detectedRowsY;
        private int detectedRowsH;
        private boolean isDraggingScrollbar;
        private double dragStartY;
        private int dragStartOffset;
        private int scrollbarTrackX;
        private int scrollbarTrackY;
        private int scrollbarTrackW;
        private int scrollbarTrackH;

        CompatTabbedListEntry(NourishedConfig config, Map<String, CompatPending> compatPending) {
            super(
                    Component.translatable("config.nourished.compat.title"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.compat.desc")}),
                    false
            );
            this.compatPending = compatPending;
            this.detectedRows = new ArrayList<>(ModCompat.getCompatReport());
            this.builtInRows = new ArrayList<>(ModCompat.getBuiltInEntries());
            this.settingsRows = new ArrayList<>(compatPending.keySet());
            this.settingsRows.sort(String::compareTo);
            for (CompatEntry row : builtInRows) {
                builtInByModId.put(row.modId(), row);
            }

            this.detectedTabLabel = Component.translatable("config.nourished.compat.tab.detected").getString();
            this.builtInTabLabel = Component.translatable("config.nourished.compat.tab.builtin").getString();
            this.settingsTabLabel = Component.translatable("config.nourished.compat.tab.settings").getString();

            this.detectedTabButton = buildTabButton(Component.literal(detectedTabLabel), 0);
            this.builtInTabButton = buildTabButton(Component.literal(builtInTabLabel), 1);
            this.settingsTabButton = buildTabButton(Component.literal(settingsTabLabel), 2);

            for (String modid : settingsRows) {
                codeButtons.put(modid, buildToggleButton(modid, true));
                tagButtons.put(modid, buildToggleButton(modid, false));
            }
            this.builtInCodeButton = buildInlineBuiltInToggleButton(true);
            this.builtInTagButton = buildInlineBuiltInToggleButton(false);
            this.detectedSearchBox = new EditBox(
                    Minecraft.getInstance().font,
                    0,
                    0,
                    120,
                    18,
                    Component.literal("Search mods"));
            this.detectedSearchBox.setHint(Component.literal("Search name or mod id"));
            this.detectedSearchBox.setResponder(s -> {
                if (!s.equals(lastSearchText)) {
                    lastSearchText = s;
                    scrollOffset = 0;
                    requestReferenceRebuilding();
                }
            });
            this.detectedSortNameButton = Button.builder(Component.literal("Name \u2191"), b -> cycleDetectedSortName())
                    .bounds(0, 0, 70, 18)
                    .build();
            this.detectedSortStatusButton = Button.builder(Component.literal("Status"), b -> cycleDetectedSortStatus())
                    .bounds(0, 0, 62, 18)
                    .build();
            this.detectedSortCategoryButton = Button.builder(Component.literal("Category"), b -> cycleDetectedSortCategory())
                    .bounds(0, 0, 78, 18)
                    .build();
            this.openDatapackFolderButton = Button.builder(Component.literal("\uD83D\uDCC2 Open Datapack Folder"), b -> openDatapackFolder())
                    .bounds(0, 0, 140, 18)
                    .build();
        }

        private Button buildTabButton(Component label, int idx) {
            return Button.builder(label, b -> {
                        tabIndex = idx;
                        scrollOffset = 0;
                    })
                    .bounds(0, 0, 100, 20)
                    .build();
        }

        private Button buildToggleButton(String modid, boolean code) {
            return Button.builder(Component.empty(), b -> {
                        CompatPending pending = compatPending.get(modid);
                        if (pending == null) return;
                        if (code) {
                            pending.codeCompat.set(!pending.codeCompat.get());
                        } else {
                            pending.tagCompat.set(!pending.tagCompat.get());
                        }
                        updateToggleLabel(modid, code);
                    })
                    .bounds(0, 0, 90, 18)
                    .build();
        }

        private Button buildInlineBuiltInToggleButton(boolean code) {
            return Button.builder(Component.empty(), b -> {
                        if (expandedBuiltInModId == null) return;
                        CompatPending pending = compatPending.get(expandedBuiltInModId);
                        if (pending == null) return;
                        if (code) {
                            pending.codeCompat.set(!pending.codeCompat.get());
                        } else {
                            pending.tagCompat.set(!pending.tagCompat.get());
                        }
                        updateBuiltInPanelButtonLabels();
                    })
                    .bounds(0, 0, 90, 18)
                    .build();
        }

        private void updateToggleLabel(String modid, boolean code) {
            CompatPending pending = compatPending.get(modid);
            Button btn = code ? codeButtons.get(modid) : tagButtons.get(modid);
            if (pending == null || btn == null) return;
            boolean value = code ? pending.codeCompat.get() : pending.tagCompat.get();
            btn.setMessage(Component.literal((code ? "Code " : "Tag ") + (value ? "ON" : "OFF")));
        }

        private void updateBuiltInPanelButtonLabels() {
            if (expandedBuiltInModId == null) {
                builtInCodeButton.setMessage(Component.literal("Code Compat OFF"));
                builtInTagButton.setMessage(Component.literal("Tag Compat OFF"));
                return;
            }
            CompatPending pending = compatPending.get(expandedBuiltInModId);
            boolean codeOn = pending != null && pending.codeCompat.get();
            boolean tagOn = pending != null && pending.tagCompat.get();
            builtInCodeButton.setMessage(Component.literal("Code Compat " + (codeOn ? "ON" : "OFF")));
            builtInTagButton.setMessage(Component.literal("Tag Compat " + (tagOn ? "ON" : "OFF")));
        }

        private void cycleDetectedSortName() {
            if (detectedSortKey != SortKey.NAME) {
                detectedSortKey = SortKey.NAME;
                detectedNameDesc = false;
            } else if (!detectedNameDesc) {
                detectedNameDesc = true;
            } else {
                detectedSortKey = SortKey.DEFAULT;
            }
            updateDetectedSortButtonLabels();
            requestReferenceRebuilding();
        }

        private void cycleDetectedSortStatus() {
            if (detectedSortKey != SortKey.STATUS) {
                detectedSortKey = SortKey.STATUS;
                detectedStatusMissingFirst = false;
            } else if (!detectedStatusMissingFirst) {
                detectedStatusMissingFirst = true;
            } else {
                detectedSortKey = SortKey.DEFAULT;
            }
            updateDetectedSortButtonLabels();
            requestReferenceRebuilding();
        }

        private void cycleDetectedSortCategory() {
            if (detectedSortKey != SortKey.CATEGORY) {
                detectedSortKey = SortKey.CATEGORY;
                detectedCategoryDesc = false;
            } else if (!detectedCategoryDesc) {
                detectedCategoryDesc = true;
            } else {
                detectedSortKey = SortKey.DEFAULT;
            }
            updateDetectedSortButtonLabels();
            requestReferenceRebuilding();
        }

        private void updateDetectedSortButtonLabels() {
            String nameLabel = detectedSortKey == SortKey.NAME
                    ? (detectedNameDesc ? "Name \u2193" : "Name \u2191")
                    : "Name \u2191\u2193";
            String statusLabel = detectedSortKey == SortKey.STATUS
                    ? (detectedStatusMissingFirst ? "Status M\u2192L" : "Status L\u2192M")
                    : "Status";
            String categoryLabel = detectedSortKey == SortKey.CATEGORY
                    ? (detectedCategoryDesc ? "Category \u2193" : "Category \u2191")
                    : "Category";
            detectedSortNameButton.setMessage(Component.literal(nameLabel));
            detectedSortStatusButton.setMessage(Component.literal(statusLabel));
            detectedSortCategoryButton.setMessage(Component.literal(categoryLabel));
        }

        private void openDatapackFolder() {
            Path targetFolder;
            Minecraft mc = Minecraft.getInstance();
            var server = mc.getSingleplayerServer();
            if (mc.level != null && server != null) {
                String worldName = server.getWorldData().getLevelName();
                targetFolder = FMLPaths.GAMEDIR.get()
                        .resolve("saves")
                        .resolve(worldName)
                        .resolve("datapacks")
                        .resolve("nourished-generated");
            } else {
                targetFolder = FMLPaths.CONFIGDIR.get()
                        .resolve("nourished")
                        .resolve("auto_compat");
            }
            try {
                if (!Files.exists(targetFolder)) {
                    Files.createDirectories(targetFolder);
                }
                Util.getPlatform().openPath(targetFolder);
            } catch (IOException e) {
                Nourished.LOGGER.error("[Compat Config] Failed to open datapack folder: {}", targetFolder, e);
            }
        }

        private List<CompatReportEntry> filteredAndSortedDetectedRows() {
            String q = detectedSearchBox.getValue() == null ? "" : detectedSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<CompatReportEntry> rows = new ArrayList<>();
            for (CompatReportEntry row : detectedRows) {
                String name = row.displayName() == null ? "" : row.displayName().toLowerCase(Locale.ROOT);
                String id = row.modId() == null ? "" : row.modId().toLowerCase(Locale.ROOT);
                if (!q.isEmpty() && !name.contains(q) && !id.contains(q)) {
                    continue;
                }
                rows.add(row);
            }
            Comparator<CompatReportEntry> byName = Comparator.comparing(
                    row -> row.displayName() == null ? row.modId() : row.displayName(),
                    String.CASE_INSENSITIVE_ORDER);
            Comparator<CompatReportEntry> byModId = Comparator.comparing(CompatReportEntry::modId, String.CASE_INSENSITIVE_ORDER);
            Comparator<CompatReportEntry> byCategory = Comparator.comparing(
                    row -> row.category() == null ? "" : row.category().name(),
                    String.CASE_INSENSITIVE_ORDER);
            Comparator<CompatReportEntry> byStatus = Comparator.comparing(CompatReportEntry::loaded).reversed();
            Comparator<CompatReportEntry> sort = switch (detectedSortKey) {
                case NAME -> (detectedNameDesc ? byName.reversed() : byName).thenComparing(byModId);
                case STATUS -> {
                    Comparator<CompatReportEntry> status = detectedStatusMissingFirst
                            ? Comparator.comparing(CompatReportEntry::loaded)
                            : byStatus;
                    yield status.thenComparing(byName);
                }
                case CATEGORY -> (detectedCategoryDesc ? byCategory.reversed() : byCategory).thenComparing(byName);
                default -> byStatus.thenComparing(byName);
            };
            rows.sort(sort);
            return rows;
        }

        private int detectedVisibleRows() {
            return Math.max(1, (VIEWPORT_H - DETECTED_TOOLBAR_H) / ROW_H);
        }

        @Override
        public boolean isEdited() {
            return true;
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
            int tabRows = rowCountForTab();
            // Only the "Detected" tab is scrollable; the other tabs size to their full content.
            int bodyH = tabIndex == 0 ? VIEWPORT_H : (tabRows * ROW_H);
            if (tabIndex == 1 && expandedBuiltInIndex >= 0) {
                bodyH += BUILTIN_PANEL_H;
            }
            return TAB_BAR_H + bodyH + 20;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
            if (tabIndex == 0
                    && mouseX >= listX && mouseX < listX + listW
                    && mouseY >= detectedRowsY && mouseY < detectedRowsY + detectedRowsH) {
                int headerHeight = DETECTED_TOOLBAR_H;
                int availableHeight = VIEWPORT_H;
                int visibleRowCount = Math.max(1, (availableHeight - headerHeight) / ROW_H);
                int maxOffset = Math.max(0, filteredAndSortedDetectedRows().size() - visibleRowCount);
                int deltaSteps = (int) -deltaY;
                if (deltaSteps == 0 && deltaY != 0.0d) {
                    deltaSteps = deltaY > 0 ? -1 : 1;
                }
                scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + deltaSteps));
                requestReferenceRebuilding();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        private int rowCountForTab() {
            return switch (tabIndex) {
                case 1 -> builtInRows.size();
                case 2 -> settingsRows.size();
                default -> detectedRows.size();
            };
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
                float delta
        ) {
            int innerX = x + 4;
            int innerW = entryWidth - 8;
            int tabW = (innerW - GAP * 2) / 3;

            detectedTabButton.setX(innerX);
            detectedTabButton.setY(y);
            detectedTabButton.setWidth(tabW);
            builtInTabButton.setX(innerX + tabW + GAP);
            builtInTabButton.setY(y);
            builtInTabButton.setWidth(tabW);
            settingsTabButton.setX(innerX + (tabW + GAP) * 2);
            settingsTabButton.setY(y);
            settingsTabButton.setWidth(tabW);
            detectedTabButton.active = isEditable();
            builtInTabButton.active = isEditable();
            settingsTabButton.active = isEditable();
            drawTab(graphics, detectedTabButton, tabIndex == 0, detectedTabLabel);
            drawTab(graphics, builtInTabButton, tabIndex == 1, builtInTabLabel);
            drawTab(graphics, settingsTabButton, tabIndex == 2, settingsTabLabel);

            listX = innerX;
            listY = y + TAB_BAR_H;
            listW = innerW;
            listH = tabIndex == 0 ? VIEWPORT_H : rowCountForTab() * ROW_H;
            graphics.fill(listX, listY, listX + listW, listY + listH, COL_BG);
            graphics.renderOutline(listX, listY, listW, listH, 0xFF404040);

            if (tabIndex == 0) {
                ensureDetectedFoodCountsComputed();
                detectedRowsY = listY + DETECTED_TOOLBAR_H;
                detectedRowsH = Math.max(0, listH - DETECTED_TOOLBAR_H);
                int toolbarX = listX + 4;
                int toolbarY = listY + 4;
                int toolbarW = listW - 8;
                int searchW = Math.max(120, toolbarW - 220);
                detectedSearchBox.setX(toolbarX);
                detectedSearchBox.setY(toolbarY);
                detectedSearchBox.setWidth(searchW);
                detectedSearchBox.setHeight(18);
                detectedSearchBox.setEditable(isEditable());
                detectedSearchBox.render(graphics, mouseX, mouseY, delta);

                int btnY = toolbarY + 20;
                int btnX = toolbarX;
                detectedSortNameButton.setX(btnX);
                detectedSortNameButton.setY(btnY);
                detectedSortNameButton.setWidth(78);
                detectedSortNameButton.active = isEditable();
                detectedSortNameButton.render(graphics, mouseX, mouseY, delta);
                detectedSortStatusButton.setX(btnX + 82);
                detectedSortStatusButton.setY(btnY);
                detectedSortStatusButton.setWidth(80);
                detectedSortStatusButton.active = isEditable();
                detectedSortStatusButton.render(graphics, mouseX, mouseY, delta);
                detectedSortCategoryButton.setX(btnX + 166);
                detectedSortCategoryButton.setY(btnY);
                detectedSortCategoryButton.setWidth(90);
                detectedSortCategoryButton.active = isEditable();
                detectedSortCategoryButton.render(graphics, mouseX, mouseY, delta);

                graphics.enableScissor(listX, listY, listX + listW, listY + listH);
                renderDetectedRows(graphics, detectedRowsY, mouseX, mouseY);
                graphics.disableScissor();

                int openBtnW = 140;
                int openBtnH = 18;
                int openBtnX = listX + listW - openBtnW - 4;
                int openBtnY = listY + listH - openBtnH - 4;
                openDatapackFolderButton.setX(openBtnX);
                openDatapackFolderButton.setY(openBtnY);
                openDatapackFolderButton.setWidth(openBtnW);
                openDatapackFolderButton.setHeight(openBtnH);
                openDatapackFolderButton.active = isEditable();
                openDatapackFolderButton.render(graphics, mouseX, mouseY, delta);

                if (hoveredDetectedIndex >= 0) {
                    List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
                    if (hoveredDetectedIndex < rows.size()) {
                        CompatReportEntry row = rows.get(hoveredDetectedIndex);
                        List<Component> tooltip = buildDetectedTooltip(row);
                        graphics.renderTooltip(Minecraft.getInstance().font, tooltip, Optional.empty(), mouseX, mouseY);
                    }
                }
                builtInCodeButton.setY(-2000);
                builtInTagButton.setY(-2000);
            } else if (tabIndex == 1) {
                detectedRowsY = -2000;
                detectedRowsH = 0;
                detectedSearchBox.setY(-2000);
                detectedSearchBox.setFocused(false);
                detectedSortNameButton.setY(-2000);
                detectedSortStatusButton.setY(-2000);
                detectedSortCategoryButton.setY(-2000);
                openDatapackFolderButton.setY(-2000);
                renderBuiltInRows(graphics, listY, mouseX, mouseY);
            } else {
                detectedRowsY = -2000;
                detectedRowsH = 0;
                detectedSearchBox.setY(-2000);
                detectedSearchBox.setFocused(false);
                detectedSortNameButton.setY(-2000);
                detectedSortStatusButton.setY(-2000);
                detectedSortCategoryButton.setY(-2000);
                openDatapackFolderButton.setY(-2000);
                renderSettingsRows(graphics, listY, mouseX, mouseY, delta);
                builtInCodeButton.setY(-2000);
                builtInTagButton.setY(-2000);
            }

        }

        private void drawTab(GuiGraphics graphics, Button button, boolean active, String label) {
            int fill = active ? COL_TAB_ACTIVE : COL_TAB_BG;
            int border = active ? COL_TAB_BORDER_ACTIVE : COL_TAB_BORDER_INACTIVE;
            int bx = button.getX();
            int by = button.getY();
            int bw = button.getWidth();
            int bh = 20;

            graphics.fill(bx, by, bx + bw, by + bh, fill);
            graphics.renderOutline(bx, by, bw, bh, border);

            var font = Minecraft.getInstance().font;
            int tx = bx + (bw - font.width(label)) / 2;
            int ty = by + (bh - 8) / 2;
            graphics.drawString(font, label, tx, ty, 0xFFF0F0F0, false);
        }

        private void renderDetectedRows(GuiGraphics graphics, int yStart, int mouseX, int mouseY) {
            List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
            hoveredDetectedIndex = -1;
            int visibleRows = detectedVisibleRows();
            int startIndex = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows)));
            int endIndex = Math.min(rows.size(), startIndex + visibleRows + 1);
            for (int i = startIndex; i < endIndex; i++) {
                CompatReportEntry row = rows.get(i);
                int ry = yStart + (i - startIndex) * ROW_H;
                if (ry + ROW_H < listY || ry > listY + listH) {
                    continue;
                }
                String category = resolvedDetectedCategory(row);
                int tint = getCategoryColor(category);
                Nourished.LOGGER.info("[Compat Config] Category tint: category={}, color=0x{}, x={}, y={}, w={}, h={}",
                        category, Integer.toHexString(tint), listX, ry, listW, ROW_H);
                graphics.fill(listX, ry, listX + listW, ry + ROW_H, tint);
                if (mouseX >= listX && mouseX < listX + listW && mouseY >= ry && mouseY < ry + ROW_H) {
                    hoveredDetectedIndex = i;
                }
                if (flashedModId != null && flashedModId.equals(row.modId()) && System.currentTimeMillis() < flashUntilMs) {
                    graphics.fill(listX + 1, ry + 1, listX + listW - 1, ry + ROW_H - 1, 0x33FFFFFF);
                }
                int iconY = ry + (ROW_H - 16) / 2;
                renderDetectedIcon(graphics, row, listX + 4, iconY);
                drawStatusChip(graphics, listX + 26, ry + 4, row.loaded(), row.conflictLevel().ordinal() > 0);
                String modName = toTitleCase(row.displayName());
                graphics.drawString(Minecraft.getInstance().font, modName, listX + 106, ry + 5, COL_TEXT, false);
                String version = detectedModVersion(row.modId());
                String countsBadge = detectedCountBadgeText(row);
                int countsColor = detectedCountBadgeColor(row);
                graphics.drawString(Minecraft.getInstance().font, "v" + version, listX + 106, ry + 14, COL_SUBTEXT, false);
                drawConflictBadge(graphics, listX + 184, ry + 12, row.conflictLevel());
                if (!countsBadge.isEmpty()) {
                    int bw = Minecraft.getInstance().font.width(countsBadge);
                    graphics.drawString(Minecraft.getInstance().font, countsBadge, listX + listW - bw - 6, ry + 14, countsColor, false);
                }
                graphics.fill(listX, ry + ROW_H - 1, listX + listW, ry + ROW_H, COL_ROW_SEPARATOR);
            }
            renderDetectedScrollIndicator(graphics, rows.size(), visibleRows);
        }

        private void renderDetectedScrollIndicator(GuiGraphics graphics, int totalRows, int visibleRows) {
            if (detectedRowsH <= 0 || totalRows <= visibleRows) {
                scrollbarTrackX = -1;
                scrollbarTrackY = -1;
                scrollbarTrackW = 0;
                scrollbarTrackH = 0;
                return;
            }
            scrollbarTrackW = 6;
            scrollbarTrackX = listX + listW - scrollbarTrackW - 2;
            scrollbarTrackY = detectedRowsY + 1;
            scrollbarTrackH = detectedRowsH - 2;
            graphics.fill(scrollbarTrackX, scrollbarTrackY, scrollbarTrackX + scrollbarTrackW, scrollbarTrackY + scrollbarTrackH, 0x66383838);

            int maxOffset = Math.max(1, totalRows - visibleRows);
            int thumbH = Math.max(14, (int) (scrollbarTrackH * (visibleRows / (double) totalRows)));
            int thumbTravel = Math.max(1, scrollbarTrackH - thumbH);
            int thumbY = scrollbarTrackY + (int) (thumbTravel * (scrollOffset / (double) maxOffset));
            graphics.fill(scrollbarTrackX, thumbY, scrollbarTrackX + scrollbarTrackW, thumbY + thumbH, 0xCC5DA9DE);
            graphics.renderOutline(scrollbarTrackX - 1, scrollbarTrackY - 1, scrollbarTrackW + 2, scrollbarTrackH + 2, 0x884A4A4A);
        }

        private void renderDetectedIcon(GuiGraphics graphics, CompatReportEntry row, int x, int y) {
            ResourceLocation logo = modLogoCache.computeIfAbsent(row.modId(), id -> resolveModLogo(id).orElse(null));
            if (logo != null) {
                graphics.blit(logo, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
            int fallback = getSolidCategoryColor(resolvedDetectedCategory(row));
            graphics.fill(x, y, x + 16, y + 16, fallback);
            graphics.renderOutline(x, y, 16, 16, 0xAA000000);
        }

        private Optional<ResourceLocation> resolveModLogo(String modId) {
            Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(modId);
            if (modContainer.isEmpty()) {
                return Optional.empty();
            }
            Optional<String> logoPath = modContainer.get().getModInfo().getLogoFile();
            if (logoPath.isEmpty()) {
                return Optional.empty();
            }
            String raw = logoPath.get();
            List<ResourceLocation> candidates = new ArrayList<>();
            if (raw.contains(":")) {
                candidates.add(ResourceLocation.parse(raw));
            } else {
                candidates.add(ResourceLocation.fromNamespaceAndPath(modId, raw));
                candidates.add(ResourceLocation.fromNamespaceAndPath(modId, "textures/" + raw));
                candidates.add(ResourceLocation.fromNamespaceAndPath(modId, "textures/gui/" + raw));
            }
            for (ResourceLocation candidate : candidates) {
                if (Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent()) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }

        private String resolvedDetectedCategory(CompatReportEntry row) {
            CompatEntry builtIn = builtInByModId.get(row.modId());
            if (builtIn != null && builtIn.category() != null) {
                return builtIn.category().name().toLowerCase(Locale.ROOT);
            }
            if (row.category() == null) {
                Nourished.LOGGER.debug("[Compat Config] Null category for {}, defaulting to FOOD_MOD", row.modId());
                return "food_mod";
            }
            return row.category().name().toLowerCase(Locale.ROOT);
        }

        private int getCategoryColor(String category) {
            return switch (category) {
                case "food_mod" -> 0x332C7F2C;
                case "farming_mod" -> 0x339C7A18;
                case "survival_overhaul" -> 0x338A2F2F;
                default -> 0;
            };
        }

        private int getSolidCategoryColor(String category) {
            return switch (category) {
                case "food_mod" -> 0xFF2C7F2C;
                case "farming_mod" -> 0xFF9C7A18;
                case "survival_overhaul" -> 0xFF8A2F2F;
                default -> 0xFF4E5C6A;
            };
        }

        private String detectedModVersion(String modId) {
            return ModList.get().getModContainerById(modId)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");
        }

        private void drawConflictBadge(GuiGraphics graphics, int x, int y, dev.maire.nourished.compat.ConflictLevel level) {
            String text;
            int bgColor;
            int borderColor;
            if (level == dev.maire.nourished.compat.ConflictLevel.FULL_CONFLICT) {
                text = "FULL CONFLICT";
                bgColor = 0xFF6B1A1A;
                borderColor = 0xFF8A2F2F;
            } else if (level == dev.maire.nourished.compat.ConflictLevel.PARTIAL_CONFLICT) {
                text = "PARTIAL";
                bgColor = 0xFF7A5A00;
                borderColor = 0xFF9C7A18;
            } else {
                text = "NONE";
                bgColor = 0xFF333333;
                borderColor = 0xFF555555;
            }
            int chipW = Math.max(52, Minecraft.getInstance().font.width(text) + 8);
            int chipH = 12;
            graphics.fill(x, y, x + chipW, y + chipH, bgColor);
            graphics.renderOutline(x, y, chipW, chipH, borderColor);
            int textX = x + (chipW - Minecraft.getInstance().font.width(text)) / 2;
            int textY = y + (chipH - 8) / 2;
            graphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFFFF, false);
        }

        private void ensureDetectedFoodCountsComputed() {
            if (detectedFoodCountsComputed) {
                return;
            }
            detectedFoodCountsComputed = true;
            for (CompatReportEntry row : detectedRows) {
                if (!row.loaded()) {
                    continue;
                }
                int totalFood = 0;
                int classified = 0;
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation id = NourishedRegistryUtils.itemKey(item);
                    if (id == null || !row.modId().equals(id.getNamespace())) {
                        continue;
                    }
                    ItemStack stack = item.getDefaultInstance();
                    FoodProperties food = stack.getFoodProperties(null);
                    if (food == null) {
                        continue;
                    }
                    totalFood++;
                    Map<String, Float> bars = FoodNutritionRegistry.resolveNutrientBars(stack, false);
                    if (bars != null && !bars.isEmpty()) {
                        classified++;
                    }
                }
                detectedFoodCounts.put(row.modId(), new int[]{classified, totalFood});
            }
        }

        private String detectedCountBadgeText(CompatReportEntry row) {
            if (!row.loaded()) return "";
            int[] counts = detectedFoodCounts.get(row.modId());
            if (counts == null) return "[0/0 classified]";
            return "[" + counts[0] + "/" + counts[1] + " classified]";
        }

        private int detectedCountBadgeColor(CompatReportEntry row) {
            int[] counts = detectedFoodCounts.get(row.modId());
            if (counts == null || counts[1] <= 0) return COL_SUBTEXT;
            double ratio = counts[0] / (double) counts[1];
            if (ratio >= 1.0d) return 0xFF72D172;
            if (ratio > 0.5d) return 0xFFE0C15C;
            return 0xFFDD7272;
        }

        private List<Component> buildDetectedTooltip(CompatReportEntry row) {
            List<Component> out = new ArrayList<>();
            CompatEntry builtIn = builtInByModId.get(row.modId());
            out.add(Component.literal(toTitleCase(row.displayName())).withStyle(s -> s.withBold(true)));
            out.add(Component.literal("ID: " + row.modId()));
            out.add(Component.literal("Version: " + detectedModVersion(row.modId())));
            out.add(Component.literal("Category: " + (row.category() == null ? "UNKNOWN" : row.category().name())));
            String namespaces = builtIn != null && builtIn.namespaces() != null && !builtIn.namespaces().isEmpty()
                    ? String.join(", ", builtIn.namespaces())
                    : row.modId();
            out.add(Component.literal("Namespaces: " + namespaces));
            String conflictSummary = "none";
            if (builtIn != null && builtIn.conflictBehavior() != null) {
                List<String> bits = new ArrayList<>();
                if (builtIn.conflictBehavior().disableEffects()) bits.add("effects disabled");
                if (builtIn.conflictBehavior().disableDecay()) bits.add("decay disabled");
                if (builtIn.conflictBehavior().disableMemory()) bits.add("memory disabled");
                if (builtIn.conflictBehavior().disableHud()) bits.add("hud disabled");
                if (!bits.isEmpty()) conflictSummary = String.join(", ", bits);
            }
            out.add(Component.literal("Conflict behavior: " + conflictSummary));
            int[] counts = detectedFoodCounts.get(row.modId());
            int foodCount = counts == null ? 0 : counts[1];
            out.add(Component.literal("Food item count: " + foodCount));
            return out;
        }

        private void renderBuiltInRows(GuiGraphics graphics, int yStart, int mouseX, int mouseY) {
            int cy = yStart;
            for (int i = 0; i < builtInRows.size(); i++) {
                CompatEntry row = builtInRows.get(i);
                int ry = cy;
                if (ry + ROW_H < listY || ry > listY + listH) {
                    if (expandedBuiltInIndex == i) {
                        builtInCodeButton.setY(-2000);
                        builtInTagButton.setY(-2000);
                    }
                    cy += ROW_H + (expandedBuiltInIndex == i ? BUILTIN_PANEL_H : 0);
                    continue;
                }
                drawCategoryChip(graphics, listX + 4, ry + 4, row.category().name());
                graphics.drawString(Minecraft.getInstance().font, toTitleCase(row.displayName()), listX + 84, ry + 5, COL_TEXT, false);
                graphics.drawString(Minecraft.getInstance().font, builtInSummary(row), listX + 84, ry + 14, COL_SUBTEXT, false);
                graphics.fill(listX, ry + ROW_H - 1, listX + listW, ry + ROW_H, COL_ROW_SEPARATOR);
                if (expandedBuiltInIndex == i) {
                    renderBuiltInSubPanel(graphics, row, ry + ROW_H, mouseX, mouseY);
                }
                cy += ROW_H + (expandedBuiltInIndex == i ? BUILTIN_PANEL_H : 0);
            }
            if (expandedBuiltInIndex < 0) {
                builtInCodeButton.setY(-2000);
                builtInTagButton.setY(-2000);
            }
        }

        private void renderBuiltInSubPanel(GuiGraphics graphics, CompatEntry row, int panelY, int mouseX, int mouseY) {
            int px = listX + 2;
            int pw = listW - 4;
            int py = panelY;
            int ph = BUILTIN_PANEL_H;
            graphics.fill(px, py, px + pw, py + ph, 0x5518202A);
            graphics.renderOutline(px, py, pw, ph, COL_TAB_BORDER_ACTIVE);
            graphics.drawString(Minecraft.getInstance().font, toTitleCase(row.displayName()), px + 6, py + 4, COL_TEXT, false);
            drawCategoryChip(graphics, px + 150, py + 3, row.category().name());

            updateBuiltInPanelButtonLabels();
            int btnW = 110;
            int btnGap = 6;
            int btnY = py + 17;
            int btnX = px + 6;
            builtInCodeButton.setX(btnX);
            builtInCodeButton.setY(btnY);
            builtInCodeButton.setWidth(btnW);
            builtInCodeButton.active = isToggleEditable(row.modId(), true);
            builtInCodeButton.render(graphics, mouseX, mouseY, 0.0f);
            builtInTagButton.setX(btnX + btnW + btnGap);
            builtInTagButton.setY(btnY);
            builtInTagButton.setWidth(btnW);
            builtInTagButton.active = isToggleEditable(row.modId(), false);
            builtInTagButton.render(graphics, mouseX, mouseY, 0.0f);
        }

        private void renderSettingsRows(GuiGraphics graphics, int yStart, int mouseX, int mouseY, float delta) {
            for (int i = 0; i < settingsRows.size(); i++) {
                String modid = settingsRows.get(i);
                int ry = yStart + i * ROW_H;
                Button codeBtn = codeButtons.get(modid);
                Button tagBtn = tagButtons.get(modid);
                if (ry + ROW_H < listY || ry > listY + listH) {
                    if (codeBtn != null) codeBtn.setY(-2000);
                    if (tagBtn != null) tagBtn.setY(-2000);
                    continue;
                }

                updateToggleLabel(modid, true);
                updateToggleLabel(modid, false);
                graphics.drawString(Minecraft.getInstance().font, toTitleCase(modid), listX + 4, ry + 6, COL_TEXT, false);

                int btnW = 92;
                int btnGap = 4;
                int rightX = listX + listW - (btnW * 2 + btnGap + 4);
                if (codeBtn != null) {
                    codeBtn.setX(rightX);
                    codeBtn.setY(ry + 3);
                    codeBtn.setWidth(btnW);
                    codeBtn.active = isToggleEditable(modid, true);
                    codeBtn.render(graphics, mouseX, mouseY, delta);
                }
                if (tagBtn != null) {
                    tagBtn.setX(rightX + btnW + btnGap);
                    tagBtn.setY(ry + 3);
                    tagBtn.setWidth(btnW);
                    tagBtn.active = isToggleEditable(modid, false);
                    tagBtn.render(graphics, mouseX, mouseY, delta);
                }
                graphics.fill(listX, ry + ROW_H - 1, listX + listW, ry + ROW_H, COL_ROW_SEPARATOR);
            }
        }

        private boolean isToggleEditable(String modid, boolean code) {
            String key = "compat." + modid + "." + (code ? "enableCodeCompat" : "enableTagCompat");
            if (LockRegistry.isLocked(key)) {
                return false;
            }
            return !(LockRegistry.isServerOnly(key) && isMultiplayer()) && isEditable();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (tabIndex == 0 && button == 0 && scrollbarTrackW > 0
                    && mouseX >= scrollbarTrackX && mouseX < scrollbarTrackX + scrollbarTrackW
                    && mouseY >= scrollbarTrackY && mouseY < scrollbarTrackY + scrollbarTrackH) {
                isDraggingScrollbar = true;
                dragStartY = mouseY;
                dragStartOffset = scrollOffset;
                return true;
            }
            if (tabIndex == 0 && button == 0 && mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
                int yStart = listY + DETECTED_TOOLBAR_H;
                int visibleRows = detectedVisibleRows();
                int startIndex = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows)));
                int endIndex = Math.min(rows.size(), startIndex + visibleRows + 1);
                for (int i = startIndex; i < endIndex; i++) {
                    int ry = yStart + (i - startIndex) * ROW_H;
                    if (mouseY >= ry && mouseY < ry + ROW_H) {
                        CompatReportEntry row = rows.get(i);
                        Minecraft.getInstance().keyboardHandler.setClipboard(row.modId());
                        flashedModId = row.modId();
                        flashUntilMs = System.currentTimeMillis() + 500L;
                        return true;
                    }
                }
            }
            if (tabIndex == 1 && button == 0 && mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                int cy = listY;
                for (int i = 0; i < builtInRows.size(); i++) {
                    int rowTop = cy;
                    int rowBottom = cy + ROW_H;
                    if (mouseY >= rowTop && mouseY < rowBottom) {
                        if (expandedBuiltInIndex == i) {
                            expandedBuiltInIndex = -1;
                            expandedBuiltInModId = null;
                        } else {
                            expandedBuiltInIndex = i;
                            expandedBuiltInModId = builtInRows.get(i).modId();
                        }
                        requestReferenceRebuilding();
                        return true;
                    }
                    cy += ROW_H;
                    if (expandedBuiltInIndex == i) {
                        cy += BUILTIN_PANEL_H;
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (isDraggingScrollbar && tabIndex == 0 && scrollbarTrackH > 0) {
                List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
                int visibleRows = detectedVisibleRows();
                int totalRows = rows.size();
                int maxOffset = Math.max(0, totalRows - visibleRows);
                if (maxOffset > 0) {
                    double dragDelta = mouseY - dragStartY;
                    int newOffset = dragStartOffset + (int) (dragDelta / scrollbarTrackH * totalRows);
                    scrollOffset = Math.max(0, Math.min(maxOffset, newOffset));
                    requestReferenceRebuilding();
                }
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (isDraggingScrollbar) {
                isDraggingScrollbar = false;
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        private void drawStatusChip(GuiGraphics graphics, int x, int y, boolean loaded, boolean conflict) {
            int chipW = 54;
            int chipH = 14;
            int col = conflict ? COL_CHIP_YELLOW : (loaded ? COL_CHIP_GREEN : COL_CHIP_RED);
            String text = conflict ? "CONFLICT" : (loaded ? "LOADED" : "MISSING");
            graphics.fill(x, y, x + chipW, y + chipH, col);
            graphics.renderOutline(x, y, chipW, chipH, COL_CHIP_BORDER);
            graphics.drawString(Minecraft.getInstance().font, text, x + 4, y + 3, 0xFFF0F0F0, false);
        }

        private void drawCategoryChip(GuiGraphics graphics, int x, int y, String category) {
            int chipW = 72;
            int chipH = 14;
            graphics.fill(x, y, x + chipW, y + chipH, COL_TAB_ACTIVE);
            graphics.renderOutline(x, y, chipW, chipH, COL_CHIP_BORDER);
            String shortCat = category.length() > 10 ? category.substring(0, 10) : category;
            graphics.drawString(Minecraft.getInstance().font, shortCat, x + 4, y + 3, 0xFFF0F0F0, false);
        }

        private String builtInSummary(CompatEntry entry) {
            List<String> bullets = new ArrayList<>();
            if (entry.providesFoodTags()) bullets.add("provides tags");
            if (entry.handlesOwnNutrition()) bullets.add("handles own nutrition");
            if (entry.conflictBehavior() != null) bullets.add("conflict rules");
            if (bullets.isEmpty()) bullets.add("baseline compat mapping");
            return String.join(", ", bullets);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            List<net.minecraft.client.gui.components.events.GuiEventListener> out = new ArrayList<>();
            out.add(detectedTabButton);
            out.add(builtInTabButton);
            out.add(settingsTabButton);
            out.add(detectedSearchBox);
            out.add(detectedSortNameButton);
            out.add(detectedSortStatusButton);
            out.add(detectedSortCategoryButton);
            out.add(openDatapackFolderButton);
            out.add(builtInCodeButton);
            out.add(builtInTagButton);
            out.addAll(codeButtons.values());
            out.addAll(tagButtons.values());
            return out;
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            List<net.minecraft.client.gui.narration.NarratableEntry> out = new ArrayList<>();
            out.add(detectedTabButton);
            out.add(builtInTabButton);
            out.add(settingsTabButton);
            out.add(detectedSearchBox);
            out.add(detectedSortNameButton);
            out.add(detectedSortStatusButton);
            out.add(detectedSortCategoryButton);
            out.add(openDatapackFolderButton);
            out.add(builtInCodeButton);
            out.add(builtInTagButton);
            out.addAll(codeButtons.values());
            out.addAll(tagButtons.values());
            return out;
        }

        private enum SortKey {
            DEFAULT,
            NAME,
            STATUS,
            CATEGORY
        }
    }

    private static final class StyledChipTextEntry extends TooltipListEntry<Object> {
        private static final int HEIGHT = 22;
        private static final int BG = 0x661C1C1C;
        private static final int TEXT = 0xFFE0E0E0;
        private final Component label;
        private final int borderColor;

        StyledChipTextEntry(Component label, int borderColor) {
            super(label, Optional::empty, false);
            this.label = label;
            this.borderColor = borderColor;
        }

        @Override
        public boolean isEdited() {
            return true;
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
            return HEIGHT;
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
                float delta
        ) {
            graphics.fill(x, y, x + entryWidth, y + HEIGHT - 1, BG);
            graphics.renderOutline(x, y, entryWidth, HEIGHT - 1, borderColor);
            graphics.drawString(Minecraft.getInstance().font, label, x + 6, y + 7, TEXT, false);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of();
        }
    }

    private static final class ModuleBulkToggleListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_HEIGHT = 20;
        private static final int GAP = 6;
        private static final long CONFIRM_WINDOW_MS = 5000L;

        private final List<String> editableModuleKeys;
        private final Map<String, AtomicBoolean> modulePending;
        private final Button enableAllButton;
        private final Button disableAllButton;
        private boolean disableConfirmArmed;
        private long disableConfirmArmedAt;

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
            this.disableAllButton = Button.builder(Component.translatable("config.nourished.modules.disableAll"), b -> onDisableAllClick())
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

        private void onDisableAllClick() {
            long now = System.currentTimeMillis();
            if (!disableConfirmArmed || now - disableConfirmArmedAt > CONFIRM_WINDOW_MS) {
                disableConfirmArmed = true;
                disableConfirmArmedAt = now;
                return;
            }
            setAll(false);
            disableConfirmArmed = false;
        }

        private void updateLabels() {
            long now = System.currentTimeMillis();
            if (disableConfirmArmed && now - disableConfirmArmedAt > CONFIRM_WINDOW_MS) {
                disableConfirmArmed = false;
            }
            disableAllButton.setMessage(disableConfirmArmed
                    ? Component.translatable("config.nourished.confirm.disableAll")
                    : Component.translatable("config.nourished.modules.disableAll"));
        }

        @Override
        public boolean isEdited() {
            return true;
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
            updateLabels();
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
        private final Button gameplayButton;

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
            this.gameplayButton = Button.builder(Component.translatable("config.nourished.modules.profile.gameplay"), b -> applyProfile("gameplay"))
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
                case "minimalist" -> setModules(true, "enableDecay", "enableNutritionEating", "enableEffects", "enableCalorieTracking");
                case "immersive" -> setModules(true, editableModuleKeys.toArray(new String[0]));
                case "gameplay" -> setModules(true,
                        "enableDecay",
                        "enableNutritionEating",
                        "enableEffects",
                        "enableCalorieTracking",
                        "enableSleepBonus");
                default -> setModules(true,
                        "enableDecay",
                        "enableNutritionEating",
                        "enableEffects",
                        "enableHUD",
                        "enableToasts",
                        "enableFoodTooltips",
                        "enableCalorieTracking",
                        "enableDietScreen",
                        "enableCriticalToasts",
                        "enableSleepBonus",
                        "enableSynergies",
                        "enableMilestones",
                        "enableSeasonHooks",
                        "enableAbsorptionModifiers");
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
            return true;
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
            return 50;
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
            balancedButton.active = isEditable();
            minimalistButton.active = isEditable();
            immersiveButton.active = isEditable();
            gameplayButton.active = isEditable();
            balancedButton.setX(x);
            balancedButton.setY(y);
            balancedButton.setWidth(btnWidth);
            minimalistButton.setX(x + btnWidth + GAP);
            minimalistButton.setY(y);
            minimalistButton.setWidth(btnWidth);
            immersiveButton.setX(x);
            immersiveButton.setY(y + BUTTON_HEIGHT + GAP);
            immersiveButton.setWidth(btnWidth);
            gameplayButton.setX(x + btnWidth + GAP);
            gameplayButton.setY(y + BUTTON_HEIGHT + GAP);
            gameplayButton.setWidth(btnWidth);
            balancedButton.render(graphics, mouseX, mouseY, delta);
            minimalistButton.render(graphics, mouseX, mouseY, delta);
            immersiveButton.render(graphics, mouseX, mouseY, delta);
            gameplayButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(balancedButton, minimalistButton, immersiveButton, gameplayButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(balancedButton, minimalistButton, immersiveButton, gameplayButton);
        }
    }

    private static final class ModuleToggleListEntry extends TooltipListEntry<Boolean> {
        private static final int PILL_WIDTH = 130;
        private static final int PILL_HEIGHT = 18;
        private static final int COL_LABEL = 0xFFE0E0E0;
        private static final int COL_HINT = 0xFFCC8844;
        private static final int COL_ON_TEXT = 0xFFB8F2B8;
        private static final int COL_OFF_TEXT = 0xFFF0B2B2;
        private static final int COL_CHIP_ON = 0xFF2C7F2C;
        private static final int COL_CHIP_OFF = 0xFF8A2F2F;
        private static final int COL_CHIP_BORDER = 0xFF1A1A1A;
        private static final int COL_CHIP_HOVER = 0x22FFFFFF;
        private static final int COL_ROW_SEPARATOR = 0x223A3A3A;
        private static final int COL_GROUP_BG = 0x55244A6C;
        private static final int COL_GROUP_BORDER = 0xAA5DA9DE;

        private final AtomicBoolean pending;
        private final String group;
        private final String dependsOnKey;
        private final Map<String, AtomicBoolean> modulePending;
        private final Component label;
        private int pillX;
        private int pillY;
        private int pillW = PILL_WIDTH;
        private int pillH = PILL_HEIGHT;
        private boolean pillPressed;
        private float hoverAlpha;
        private long lastHoverUpdateMs;

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
        }

        private void togglePending() {
            boolean next = !this.pending.get();
            if (next && dependsOnKey != null) {
                AtomicBoolean dep = modulePending.get(dependsOnKey);
                if (dep != null && !dep.get()) {
                    dep.set(true);
                }
            }
            this.pending.set(next);
        }

        private static String groupBadge(String group) {
            return switch (group) {
                case "core" -> "C";
                case "ui" -> "UI";
                default -> "+";
            };
        }

        @Override
        public boolean isEdited() {
            return true;
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
            pillX = x + entryWidth - PILL_WIDTH;
            pillY = y + 1;
            pillW = PILL_WIDTH;
            pillH = PILL_HEIGHT;
            int iconW = 24;
            int iconH = 14;
            int iconX = x;
            int iconY = y + 4;
            graphics.fill(iconX, iconY, iconX + iconW, iconY + iconH, COL_GROUP_BG);
            graphics.renderOutline(iconX, iconY, iconW, iconH, COL_GROUP_BORDER);
            String badge = groupBadge(group);
            int badgeX = iconX + (iconW - Minecraft.getInstance().font.width(badge)) / 2;
            graphics.drawString(Minecraft.getInstance().font, badge, badgeX, iconY + 3, 0xFFE8F4FF, false);
            graphics.drawString(Minecraft.getInstance().font, label, x + iconW + 6, y + 6, COL_LABEL, false);

            int chipX = x + 188;
            int chipY = y + 4;
            int chipW = 74;
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
            if (dependsOnKey != null) {
                AtomicBoolean dep = modulePending.get(dependsOnKey);
                if (dep != null && !dep.get()) {
                    String depLabel = Component.translatable("config.nourished." + dependsOnKey).getString();
                    String depText = Component.translatable("config.nourished.modules.requires", depLabel).getString();
                    graphics.drawString(Minecraft.getInstance().font, depText, x + 188, y + 15, COL_HINT, false);
                }
            }
            boolean pillHovered = mouseX >= pillX && mouseX < pillX + pillW && mouseY >= pillY && mouseY < pillY + pillH;
            long now = System.currentTimeMillis();
            if (lastHoverUpdateMs == 0L) {
                lastHoverUpdateMs = now;
            }
            float dt = Math.min(0.05f, (now - lastHoverUpdateMs) / 1000.0f);
            lastHoverUpdateMs = now;
            float target = pillHovered ? 1.0f : 0.0f;
            float speed = 10.0f;
            hoverAlpha += (target - hoverAlpha) * Math.min(1.0f, dt * speed);
            int drawOffset = pillPressed ? 1 : 0;
            int px = pillX;
            int py = pillY + drawOffset;
            int pillBg = isEditable() ? (pillPressed ? 0xFF1D4258 : 0xFF234F6B) : 0xFF3A3A3A;
            graphics.fill(px, py, px + pillW, py + pillH, pillBg);
            graphics.renderOutline(px, py, pillW, pillH, 0xFF5DA9DE);
            if (!pillPressed && hoverAlpha > 0.01f) {
                int a = Math.max(0, Math.min(255, (int) (hoverAlpha * 0x22)));
                graphics.fill(px, py, px + pillW, py + pillH, (a << 24) | 0x00FFFFFF);
            }
            String pillText = Component.translatable("config.nourished.modules.toggle").getString();
            int pillTextX = px + (pillW - Minecraft.getInstance().font.width(pillText)) / 2;
            graphics.drawString(Minecraft.getInstance().font, pillText, pillTextX, py + 5, 0xFFE8F4FF, false);
            graphics.fill(x, y + 23, x + entryWidth, y + 24, COL_ROW_SEPARATOR);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isEditable()
                    && mouseX >= pillX && mouseX < pillX + pillW
                    && mouseY >= pillY && mouseY < pillY + pillH) {
                pillPressed = true;
                togglePending();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) {
                pillPressed = false;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of();
        }
    }

    private record ModuleMeta(String key, String group, String dependsOn) {}

    private static final class HudQuickActionsListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_HEIGHT = 20;
        private static final int GAP = 6;
        private static final long CONFIRM_WINDOW_MS = 5000L;

        private final NourishedClientConfig client;
        private final Button resetHudPositionButton;
        private final Button resetDietOrderButton;
        private boolean resetHudConfirmArmed;
        private boolean resetOrderConfirmArmed;
        private long resetHudConfirmAt;
        private long resetOrderConfirmAt;

        HudQuickActionsListEntry(NourishedClientConfig client) {
            super(
                    Component.translatable("config.nourished.hud.quickActions"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.hud.quickActions.desc")}),
                    false);
            this.client = client;
            this.resetHudPositionButton = Button.builder(
                            Component.translatable("config.nourished.hud.resetPosition"),
                            b -> onResetHudPositionClick()
                    )
                    .bounds(0, 0, 120, BUTTON_HEIGHT)
                    .build();
            this.resetDietOrderButton = Button.builder(
                            Component.translatable("config.nourished.hud.resetDietOrder"),
                            b -> onResetDietOrderClick()
                    )
                    .bounds(0, 0, 120, BUTTON_HEIGHT)
                    .build();
        }

        private void onResetHudPositionClick() {
            long now = System.currentTimeMillis();
            if (!resetHudConfirmArmed || now - resetHudConfirmAt > CONFIRM_WINDOW_MS) {
                resetHudConfirmArmed = true;
                resetHudConfirmAt = now;
                return;
            }
            this.client.resetHudOffsets();
            resetHudConfirmArmed = false;
        }

        private void onResetDietOrderClick() {
            long now = System.currentTimeMillis();
            if (!resetOrderConfirmArmed || now - resetOrderConfirmAt > CONFIRM_WINDOW_MS) {
                resetOrderConfirmArmed = true;
                resetOrderConfirmAt = now;
                return;
            }
            this.client.resetDietBarOrder();
            resetOrderConfirmArmed = false;
        }

        private void updateLabels() {
            long now = System.currentTimeMillis();
            if (resetHudConfirmArmed && now - resetHudConfirmAt > CONFIRM_WINDOW_MS) {
                resetHudConfirmArmed = false;
            }
            if (resetOrderConfirmArmed && now - resetOrderConfirmAt > CONFIRM_WINDOW_MS) {
                resetOrderConfirmArmed = false;
            }
            resetHudPositionButton.setMessage(resetHudConfirmArmed
                    ? Component.translatable("config.nourished.confirm.resetHud")
                    : Component.translatable("config.nourished.hud.resetPosition"));
            resetDietOrderButton.setMessage(resetOrderConfirmArmed
                    ? Component.translatable("config.nourished.confirm.resetOrder")
                    : Component.translatable("config.nourished.hud.resetDietOrder"));
        }

        @Override
        public boolean isEdited() {
            return true;
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
            int btnWidth = (entryWidth - GAP) / 2;
            updateLabels();
            resetHudPositionButton.active = isEditable();
            resetDietOrderButton.active = isEditable();
            resetHudPositionButton.setX(x);
            resetHudPositionButton.setY(y);
            resetHudPositionButton.setWidth(btnWidth);
            resetDietOrderButton.setX(x + btnWidth + GAP);
            resetDietOrderButton.setY(y);
            resetDietOrderButton.setWidth(btnWidth);
            resetHudPositionButton.render(graphics, mouseX, mouseY, delta);
            resetDietOrderButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(resetHudPositionButton, resetDietOrderButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(resetHudPositionButton, resetDietOrderButton);
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
                        NourishedReloadPipeline.reloadAll();
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
            return true;
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
