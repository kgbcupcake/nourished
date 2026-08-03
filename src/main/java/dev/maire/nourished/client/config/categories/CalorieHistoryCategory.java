package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.config.categories.widgets.ModuleToggleListEntry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.client.config.cloth.ColorHexRowWidget;
import dev.marie.framework.color.ColorKey;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.buildFloatSlider;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.isMultiplayer;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.moduleToggleDescription;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.moduleToggleTitle;

public final class CalorieHistoryCategory {
    private CalorieHistoryCategory() {}
    public static void addCalorieHistoryCategory(
            NourishedConfig config,
            NourishedClientConfig client,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, AtomicBoolean> modulePending
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.calorieHistory"));

        AtomicBoolean calorieHistoryPending = modulePending.get("enableCalorieHistory");
        if (calorieHistoryPending != null && !NourishedLockRegistry.isLocked("enableCalorieHistory")) {
            var calorieHistoryEntry = new ModuleToggleListEntry(
                    moduleToggleTitle("enableCalorieHistory"),
                    moduleToggleDescription("enableCalorieHistory"),
                    calorieHistoryPending,
                    "other",
                    null,
                    modulePending);
            boolean editable = !(NourishedLockRegistry.isServerOnly("enableCalorieHistory") && isMultiplayer());
            if (!editable) {
                calorieHistoryEntry.setEditable(false);
            }
            category.addEntry(calorieHistoryEntry);
        }

        if (!NourishedLockRegistry.isLocked("calorieHistoryRetentionDays")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.calorieHistoryRetentionDays"), config.calorieHistoryRetentionDays(), 1, 90)
                            .setDefaultValue(7)
                            .setTextGetter(v -> Component.literal(String.valueOf(v)))
                            .setTooltip(Component.translatable("config.nourished.calorieHistoryRetentionDays.desc"))
                            .setSaveConsumer(config::setCalorieHistoryRetentionDays)
                            .build()
            );
        }

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.enableCalorieHistoryHud"), client.enableCalorieHistoryHud())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setEnableCalorieHistoryHud)
                        .setTooltip(Component.translatable("config.nourished.enableCalorieHistoryHud.desc"))
                        .build()
        );
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.calorieHudEditHotkey"),
                                NourishedKeys.EDIT_CALORIE_HUD.getKey()
                        )
                        .setDefaultValue(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C))
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.EDIT_CALORIE_HUD.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );
        category.addEntry(
                buildFloatSlider(
                        eb,
                        Component.translatable("config.nourished.calorieHudBackgroundOpacity"),
                        (float) client.calorieHudBackgroundOpacity(),
                        0.0f,
                        1.0f,
                        204f / 255f,
                        client::setCalorieHudBackgroundOpacity
                )
        );
        category.addEntry(new ColorHexRowWidget(
                ColorKey.of(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "panel.calorieHud")),
                Component.translatable("config.nourished.calorieHudBackgroundColor"),
                Component.translatable("config.nourished.hudColors.row.tooltip")));

        addReloadButton(category, eb, false);
    }
}
