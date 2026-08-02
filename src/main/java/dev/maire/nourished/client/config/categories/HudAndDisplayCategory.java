package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.config.HudNutrientColorsResetAllEntry;
import dev.maire.nourished.client.config.HudNutrientColorsSectionHeaderEntry;
import dev.maire.nourished.client.config.NutrientHudHexColorRowEntry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.color.ColorRegistry;
import dev.marie.framework.config.HudAnchor;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class HudAndDisplayCategory {
    private HudAndDisplayCategory() {}
    public static void addHudAndDisplayCategory(NourishedConfig config, NourishedClientConfig client, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.hud_display"));
        category.addEntry(new HudQuickActionsListEntry(client));

        category.addEntry(
                eb.startEnumSelector(Component.translatable("config.nourished.hudAnchor"), HudAnchor.class, client.hudAnchor())
                        .setDefaultValue(HudAnchor.BOTTOM_LEFT)
                        .setSaveConsumer(client::setHudAnchor)
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudOffsetX"), client.hudOffsetX(), -2000, 2000)
                        .setDefaultValue(0)
                        .setSaveConsumer(client::setHudOffsetX)
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudOffsetY"), client.hudOffsetY(), -2000, 2000)
                        .setDefaultValue(0)
                        .setSaveConsumer(client::setHudOffsetY)
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudBarWidth"), client.hudBarWidth(), 40, 120)
                        .setDefaultValue(60)
                        .setSaveConsumer(client::setHudBarWidth)
                        .build()
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.hudScale"),
                        client.hudScale(),
                        0.3d,
                        3.0d,
                        1.0d,
                        client::setHudScale
                )
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.hudReservedBottom"), client.hudReservedBottom(), 30, 100)
                        .setDefaultValue(52)
                        .setSaveConsumer(client::setHudReservedBottom)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.hudDraggable"), client.hudDraggable())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setHudDraggable)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.dietBarDragEnabled"), client.dietBarDragEnabled())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setDietBarDragEnabled)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.hudShowZeroBars"), client.hudShowZeroBars())
                        .setDefaultValue(false)
                        .setSaveConsumer(client::setHudShowZeroBars)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("nourished.config.hud.reveal_on_gain"),
                                client.hudRevealOnNutrientGain())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setHudRevealOnNutrientGain)
                        .setTooltip(Component.translatable("nourished.config.hud.reveal_on_gain.desc"))
                        .build()
        );
        IntegerSliderEntry hideAboveEntry = (IntegerSliderEntry) buildDoubleSlider(
                eb,
                Component.translatable("config.nourished.hudHideAboveThreshold"),
                client.hudHideAboveThreshold(),
                0.0d,
                1.0d,
                1.0d,
                client::setHudHideAboveThreshold,
                Component.translatable("config.nourished.hudHideAboveThreshold.desc")
        );
        category.addEntry(hideAboveEntry);
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.hudShowAboveThreshold"),
                        client.hudShowAboveThreshold(),
                        0.0d,
                        1.0d,
                        1.0d,
                        client::setHudShowAboveThreshold,
                        () -> hideAboveEntry.getValue() < 1000,
                        Component.translatable("config.nourished.hudShowAboveThreshold.desc")
                )
        );
        category.addEntry(
                buildFloatSlider(
                        eb,
                        Component.translatable("config.nourished.hudBackgroundOpacity"),
                        (float) client.hudBackgroundOpacity(),
                        0.0f,
                        1.0f,
                        204f / 255f,
                        v -> client.setHudBackgroundOpacity(v)
                )
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.hudVerticalLayout"), client.hudVerticalLayout())
                        .setDefaultValue(false)
                        .setSaveConsumer(client::setHudVerticalLayout)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.hudClassicMode"), client.hudClassicMode())
                        .setDefaultValue(false)
                        .setSaveConsumer(client::setHudClassicMode)
                        .setTooltip(Component.translatable("config.nourished.hudClassicMode.desc"))
                        .build()
        );
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
                eb.startBooleanToggle(Component.translatable("config.nourished.enableActivityLogHud"), client.enableActivityLogHud())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setEnableActivityLogHud)
                        .setTooltip(Component.translatable("config.nourished.enableActivityLogHud.desc"))
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

        category.addEntry(new HudNutrientColorsSectionHeaderEntry());
        List<NutrientHudHexColorRowEntry> hudNutrientColorRows = new ArrayList<>();
        category.addEntry(new HudNutrientColorsResetAllEntry(() -> {
            for (String valueKey : NutrientRegistry.getKeys()) {
                ColorRegistry.remove(valueKey);
            }
            for (NutrientHudHexColorRowEntry row : hudNutrientColorRows) {
                row.syncAfterBulkReset();
            }
        }));
        for (String valueKey : NutrientRegistry.getKeys()) {
            NutrientHudHexColorRowEntry row = new NutrientHudHexColorRowEntry(valueKey);
            hudNutrientColorRows.add(row);
            category.addEntry(row);
        }

        addReloadButton(category, eb, false);
    }
    static final class HudQuickActionsListEntry extends TooltipListEntry<Object> {
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
}
