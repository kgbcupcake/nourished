package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.config.NourishedClientConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class DietScreenCategory {
    private DietScreenCategory() {}
    public static void addDietScreenCategory(NourishedClientConfig client, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.diet_screen"));

        category.addEntry(new DietScreenResetPositionListEntry(client));

        List<AbstractConfigListEntry> layoutEntries = new ArrayList<>();
        layoutEntries.add(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.dietScale"),
                        client.dietScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setDietScale
                )
        );
        layoutEntries.add(
                buildFloatSlider(
                        eb,
                        Component.translatable("config.nourished.dietBackgroundOpacity"),
                        (float) client.dietBackgroundOpacity(),
                        0.0f,
                        1.0f,
                        204f / 255f,
                        v -> client.setDietBackgroundOpacity(v)
                )
        );
        layoutEntries.add(
                eb.startIntSlider(Component.translatable("config.nourished.dietOffsetX"), client.dietOffsetX(), -2000, 2000)
                        .setDefaultValue(0)
                        .setSaveConsumer(client::setDietOffsetX)
                        .build()
        );
        layoutEntries.add(
                eb.startIntSlider(Component.translatable("config.nourished.dietOffsetY"), client.dietOffsetY(), -2000, 2000)
                        .setDefaultValue(0)
                        .setSaveConsumer(client::setDietOffsetY)
                        .build()
        );
        layoutEntries.add(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.recentMealsBoxScale"),
                        client.recentMealsBoxScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setRecentMealsBoxScale
                )
        );
        layoutEntries.add(
                buildDoubleSlider(
                        eb,
                        Component.translatable("config.nourished.eatMoreBoxScale"),
                        client.eatMoreBoxScale(),
                        0.5d,
                        1.5d,
                        1.0d,
                        client::setEatMoreBoxScale
                )
        );
        category.addEntry(eb.startSubCategory(Component.translatable("config.nourished.dietScreen.group.layout"), layoutEntries).setExpanded(true).build());

        List<AbstractConfigListEntry> visibilityEntries = new ArrayList<>();
        visibilityEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.showRecentMeals"), client.showRecentMeals())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowRecentMeals)
                        .build()
        );
        visibilityEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.showEatMoreOf"), client.showEatMoreOf())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowEatMoreOf)
                        .build()
        );
        visibilityEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.showActiveEffects"), client.showActiveEffects())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowActiveEffects)
                        .build()
        );
        visibilityEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.showCaloriesBox"), client.showCaloriesBox())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowCaloriesBox)
                        .build()
        );
        visibilityEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.showBalanceBox"), client.showBalanceBox())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowBalanceBox)
                        .build()
        );
        visibilityEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.showDietScreenButton"), client.showDietScreenButton())
                        .setDefaultValue(true)
                        .setSaveConsumer(client::setShowDietScreenButton)
                        .build()
        );
        category.addEntry(eb.startSubCategory(Component.translatable("config.nourished.dietScreen.group.visibility"), visibilityEntries).setExpanded(true).build());

        List<AbstractConfigListEntry> behaviorEntries = new ArrayList<>();
        behaviorEntries.add(
                eb.startBooleanToggle(Component.translatable("config.nourished.dietScreenClassicMode"), client.dietScreenClassicMode())
                        .setDefaultValue(false)
                        .setSaveConsumer(client::setDietScreenClassicMode)
                        .build()
        );
        category.addEntry(eb.startSubCategory(Component.translatable("config.nourished.dietScreen.group.behavior"), behaviorEntries).setExpanded(false).build());

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
                    () -> Optional.empty(),
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
