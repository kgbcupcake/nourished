package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.config.NourishedClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class DietScreenCategory {
    private DietScreenCategory() {}
    public static void addDietScreenCategory(NourishedClientConfig client, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.diet_screen"));

        category.addEntry(new DietScreenResetPositionListEntry(client));

        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.dietScale"),
                        client.dietScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setDietScale,
                        Component.translatable("config.nourished.dietScale.desc")
                )
        );
        category.addEntry(
                buildFloatSlider(
                        eb,
                        Component.translatable("config.nourished.dietBackgroundOpacity"),
                        (float) client.dietBackgroundOpacity(),
                        0.0f,
                        1.0f,
                        204f / 255f,
                        v -> client.setDietBackgroundOpacity(v),
                        Component.translatable("config.nourished.dietBackgroundOpacity.desc")
                )
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.dietOffsetX"), client.dietOffsetX(), -2000, 2000)
                        .setDefaultValue(0)
                        .setSaveConsumer(client::setDietOffsetX)
                        .setTooltip(Component.translatable("config.nourished.dietOffsetX.desc"))
                        .build()
        );
        category.addEntry(
                eb.startIntSlider(Component.translatable("config.nourished.dietOffsetY"), client.dietOffsetY(), -2000, 2000)
                        .setDefaultValue(0)
                        .setSaveConsumer(client::setDietOffsetY)
                        .setTooltip(Component.translatable("config.nourished.dietOffsetY.desc"))
                        .build()
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.recentMealsBoxScale"),
                        client.recentMealsBoxScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setRecentMealsBoxScale,
                        Component.translatable("config.nourished.recentMealsBoxScale.desc")
                )
        );
        category.addEntry(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.eatMoreBoxScale"),
                        client.eatMoreBoxScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setEatMoreBoxScale,
                        Component.translatable("config.nourished.eatMoreBoxScale.desc")
                )
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showRecentMeals"), client.showRecentMeals())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowRecentMeals)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showEatMoreOf"), client.showEatMoreOf())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowEatMoreOf)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showActiveEffects"), client.showActiveEffects())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowActiveEffects)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showCaloriesBox"), client.showCaloriesBox())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowCaloriesBox)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showBalanceBox"), client.showBalanceBox())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowBalanceBox)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.showDietScreenButton"), client.showDietScreenButton())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowDietScreenButton)
                        .build()
        );
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.nourished.dietScreenClassicMode"), client.dietScreenClassicMode())
                        .setDefaultValue(false)
                        .setSaveConsumer(client::setDietScreenClassicMode)
                        .setTooltip(Component.translatable("config.nourished.dietScreenClassicMode.desc"))
                        .build()
        );
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

        addReloadButton(category, eb, false);
    }
    static final class DietScreenResetPositionListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_HEIGHT = 20;
        private static final long CONFIRM_WINDOW_MS = 5000L;

        private final NourishedClientConfig client;
        private final Button resetButton;
        private boolean confirmArmed;
        private long confirmAt;

        DietScreenResetPositionListEntry(NourishedClientConfig client) {
            super(
                    Component.translatable("config.nourished.diet.resetPosition"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.diet.resetPosition.desc")}),
                    false);
            this.client = client;
            this.resetButton = Button.builder(
                            Component.translatable("config.nourished.diet.resetPosition"),
                            b -> onResetClick()
                    )
                    .bounds(0, 0, 150, BUTTON_HEIGHT)
                    .build();
        }

        private void onResetClick() {
            long now = System.currentTimeMillis();
            if (!confirmArmed || now - confirmAt > CONFIRM_WINDOW_MS) {
                confirmArmed = true;
                confirmAt = now;
                return;
            }
            this.client.resetDietOffsets();
            confirmArmed = false;
        }

        private void updateLabel() {
            long now = System.currentTimeMillis();
            if (confirmArmed && now - confirmAt > CONFIRM_WINDOW_MS) {
                confirmArmed = false;
            }
            resetButton.setMessage(confirmArmed
                    ? Component.translatable("config.nourished.confirm.resetDiet")
                    : Component.translatable("config.nourished.diet.resetPosition"));
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
            updateLabel();
            resetButton.active = isEditable();
            resetButton.setX(x);
            resetButton.setY(y);
            resetButton.setWidth(entryWidth);
            resetButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(resetButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(resetButton);
        }
    }
}
