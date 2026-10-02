package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.NourishedKeys;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;

public final class HotkeysCategory {
    private HotkeysCategory() {}
    public static void addHotkeysCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.hotkeys"));

        category.addEntry(eb.startTextDescription(Component.translatable("config.nourished.hotkeys.group.huds")).build());
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.hudEditHotkey"),
                                NourishedKeys.EDIT_HUD.getKey()
                        )
                        .setDefaultValue(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H))
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.EDIT_HUD.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.editAllHudsHotkey"),
                                NourishedKeys.EDIT_ALL_HUDS.getKey()
                        )
                        .setDefaultValue(InputConstants.UNKNOWN)
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.EDIT_ALL_HUDS.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.activityLogHudEditHotkey"),
                                NourishedKeys.EDIT_ACTIVITY_LOG_HUD.getKey()
                        )
                        .setDefaultValue(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K))
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.EDIT_ACTIVITY_LOG_HUD.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
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

        category.addEntry(eb.startTextDescription(Component.translatable("config.nourished.hotkeys.group.screens")).build());
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.dietEditHotkey"),
                                NourishedKeys.EDIT_DIET_SCREEN.getKey()
                        )
                        .setDefaultValue(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J))
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.EDIT_DIET_SCREEN.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.dietScaleConfigHotkey"),
                                NourishedKeys.OPEN_SCALE_CONFIG.getKey()
                        )
                        .setDefaultValue(InputConstants.UNKNOWN)
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.OPEN_SCALE_CONFIG.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );

        category.addEntry(eb.startTextDescription(Component.translatable("config.nourished.hotkeys.group.other")).build());
        category.addEntry(
                eb.startKeyCodeField(
                                Component.translatable("config.nourished.commandCenterHotkey"),
                                NourishedKeys.OPEN_COMMAND_CENTER.getKey()
                        )
                        .setDefaultValue(InputConstants.UNKNOWN)
                        .setKeySaveConsumer(key -> {
                            NourishedKeys.OPEN_COMMAND_CENTER.setKey(key);
                            KeyMapping.resetMapping();
                            Minecraft.getInstance().options.save();
                        })
                        .build()
        );

        addReloadButton(category, eb, false);
    }
}
