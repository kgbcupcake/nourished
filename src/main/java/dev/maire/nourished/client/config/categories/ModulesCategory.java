package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.categories.rawfood.RawFoodConfigEntries;
import dev.maire.nourished.client.config.categories.widgets.ModuleBulkToggleListEntry;
import dev.maire.nourished.client.config.categories.widgets.ModuleProfilesListEntry;
import dev.maire.nourished.client.config.categories.widgets.ModuleToggleListEntry;
import dev.maire.nourished.client.config.categories.widgets.StyledChipTextEntry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;
import static dev.maire.nourished.client.config.categories.rawfood.RawFoodConfigEntries.addRawFoodConfigEntries;

public final class ModulesCategory {
    private ModulesCategory() {}
    public static void addModulesCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, AtomicBoolean> modulePending
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.modules"));
        List<ModuleMeta> metas = moduleMetas(config.moduleToggles());
        List<String> editableModuleKeys = new ArrayList<>();
        Map<String, List<AbstractConfigListEntry>> groupedEntries = new LinkedHashMap<>();
        for (String group : List.of("core", "rawfood", "ui", "other")) {
            groupedEntries.put(group, new ArrayList<>());
        }
        for (ModuleMeta meta : metas) {
            String key = meta.key;
            if (NourishedLockRegistry.isLocked(key)) {
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
            boolean editable = !(NourishedLockRegistry.isServerOnly(key) && isMultiplayer());
            if (!editable) {
                entry.setEditable(false);
            } else {
                editableModuleKeys.add(key);
            }
            if (!"enableDebugLogging".equals(key)) {
                groupedEntries.getOrDefault(meta.group, groupedEntries.get("other")).add(entry);
            }
        }

        if (!editableModuleKeys.isEmpty()) {
            category.addEntry(new ModuleProfilesListEntry(modulePending, editableModuleKeys));
            category.addEntry(new ModuleBulkToggleListEntry(editableModuleKeys, modulePending));
        }

        addRawFoodConfigEntries(groupedEntries.get("rawfood"), eb);

        addModuleGroupSubcategory(category, eb, "core", groupedEntries.get("core"));
        addModuleGroupSubcategory(category, eb, "rawfood", groupedEntries.get("rawfood"));
        addModuleGroupSubcategory(category, eb, "ui", groupedEntries.get("ui"));
        addModuleGroupSubcategory(category, eb, "other", groupedEntries.get("other"));

        if (!NourishedLockRegistry.isLocked("heavySourcePropertyThreshold")) {
            category.addEntry(
                    eb.startIntSlider(
                                    Component.translatable("nourished.config.heavySourcePropertyThreshold"),
                                    config.heavySourcePropertyThreshold(),
                                    1,
                                    20)
                            .setDefaultValue(6)
                            .setTooltip(Component.translatable("nourished.config.heavySourcePropertyThreshold.desc"))
                            .setSaveConsumer(config::setHeavySourcePropertyThreshold)
                            .build()
            );
        }

        if (!editableModuleKeys.isEmpty()) {
            category.addEntry(new StyledChipTextEntry(Component.translatable("config.nourished.modules.dependencyHint"), 0xFFCC8844));
        }

        addReloadButton(category, eb, false);
    }
    static void addModuleGroupSubcategory(
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

    static Component groupTitle(String group) {
        return Component.translatable(switch (group) {
            case "rawfood" -> "config.nourished.modules.group.rawfood";
            default -> "config.nourished.modules.group." + group;
        });
    }
    static List<ModuleMeta> moduleMetas(Map<String, ModConfigSpec.BooleanValue> moduleMap) {
        List<ModuleMeta> out = new ArrayList<>();
        for (String key : moduleMap.keySet()) {
            String group;
            String dependsOn = null;
            switch (key) {
                case "enableDecay", "enableSourceApplication", "enableBlockHeavySources", "enableBlockLightSource",
                "enableEffects", "enableCalorieTracking", "enableSleepBonus",
                    "enableSynergies", "enableMilestones", "enableSeasonHooks", "enableAbsorptionModifiers" -> group = "core";
                case "enableRawFoodPenalty", "enableGutHealth" -> group = "rawfood";
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
    public record ModuleMeta(String key, String group, String dependsOn) {}
}
