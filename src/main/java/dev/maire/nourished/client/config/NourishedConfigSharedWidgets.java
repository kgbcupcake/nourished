package dev.maire.nourished.client.config;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public final class NourishedConfigSharedWidgets {

    private NourishedConfigSharedWidgets() {}

    public static boolean isMultiplayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null;
    }

    public static String ellipsize(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (maxWidth <= ellipsisWidth) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    public static String ellipsize(Component text, int maxWidth) {
        return ellipsize(Minecraft.getInstance().font, text.getString(), maxWidth);
    }

    /** Split two buttons across one row, or stack vertically when {@code innerWidth} is tight. */
    public static void layoutDualButtons(
            Button left,
            Button right,
            int sx,
            int y,
            int innerWidth,
            int minSideBySideWidth,
            int gap,
            int buttonHeight
    ) {
        if (innerWidth >= minSideBySideWidth) {
            int btnWidth = Math.max(72, (innerWidth - gap) / 2);
            left.setX(sx);
            left.setY(y);
            left.setWidth(btnWidth);
            right.setX(sx + btnWidth + gap);
            right.setY(y);
            right.setWidth(btnWidth);
            return;
        }
        left.setX(sx);
        left.setY(y);
        left.setWidth(innerWidth);
        right.setX(sx);
        right.setY(y + buttonHeight + gap);
        right.setWidth(innerWidth);
    }
    public static void addReloadButton(ConfigCategory category, ConfigEntryBuilder eb, boolean includeOpenDatapackFolder) {
        category.addEntry(
                eb.startTextDescription(Component.empty()).build()
        );
        category.addEntry(new ReloadConfigsListEntry(includeOpenDatapackFolder));
    }
    public static void openCompatDatapackFolder() {
        Path targetFolder;
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.level != null && server != null) {
            targetFolder = server.getWorldPath(LevelResource.DATAPACK_DIR)
                    .resolve("nourished-generated");
        } else {
            targetFolder = FMLPaths.CONFIGDIR.get()
                    .resolve(Nourished.MODID)
                    .resolve("auto_compat");
        }
        try {
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }
            try {
                new ProcessBuilder("xdg-open", targetFolder.toAbsolutePath().toString()).start();
            } catch (Exception e) {
                Util.getPlatform().openPath(targetFolder);
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[Compat Config] Failed to open datapack folder: {}", targetFolder, e);
        }
    }
    public static String prettyKey(String key) {
        return NutrientRegistry.getLabel(key);
    }
    public static AbstractConfigListEntry buildDoubleSlider(
            ConfigEntryBuilder eb,
            Component label,
            double value,
            double min,
            double max,
            double defaultValue,
            DoubleConsumer saveConsumer,
            Component... tooltip
    ) {
        return buildDoubleSlider(eb, label, value, min, max, defaultValue, saveConsumer, null, tooltip);
    }

    public static AbstractConfigListEntry buildDoubleSlider(
            ConfigEntryBuilder eb,
            Component label,
            double value,
            double min,
            double max,
            double defaultValue,
            DoubleConsumer saveConsumer,
            Supplier<Boolean> enabledSupplier,
            Component... tooltip
    ) {
        int scale = 1000;
        int minScaled = (int) Math.round(min * scale);
        int maxScaled = (int) Math.round(max * scale);
        int valueScaled = (int) Math.round(Math.max(min, Math.min(max, value)) * scale);
        int defaultScaled = (int) Math.round(Math.max(min, Math.min(max, defaultValue)) * scale);
        if (enabledSupplier != null) {
            return new DependentIntegerSliderEntry(
                    label,
                    valueScaled,
                    minScaled,
                    maxScaled,
                    Component.translatable("text.cloth-config.reset_value"),
                    () -> defaultScaled,
                    v -> saveConsumer.accept(v / (double) scale),
                    () -> tooltip.length > 0 ? Optional.of(tooltip) : Optional.empty(),
                    false,
                    v -> Component.literal(String.format(Locale.ROOT, "%.3f", v / (double) scale)),
                    enabledSupplier
            );
        }
        var slider = eb.startIntSlider(label, valueScaled, minScaled, maxScaled)
                .setDefaultValue(defaultScaled)
                .setTextGetter(v -> Component.literal(String.format(Locale.ROOT, "%.3f", v / (double) scale)))
                .setSaveConsumer(v -> saveConsumer.accept(v / (double) scale));
        if (tooltip.length > 0) {
            slider.setTooltip(tooltip);
        }
        return slider.build();
    }

    public static AbstractConfigListEntry buildFloatSlider(
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
        var slider = eb.startIntSlider(label, valueScaled, minScaled, maxScaled)
                .setDefaultValue(defaultScaled)
                .setTextGetter(v -> Component.literal(String.format(Locale.ROOT, "%.3f", v / (float) scale)))
                .setSaveConsumer(v -> saveConsumer.accept(v / (float) scale));
        if (tooltip.length > 0) {
            slider.setTooltip(tooltip);
        }
        return slider.build();
    }

    public static AbstractConfigListEntry buildSteppedFloatSlider(
            Component label,
            float value,
            float min,
            float max,
            float step,
            float defaultValue,
            Consumer<Float> saveConsumer,
            Supplier<Boolean> enabledSupplier,
            Component... tooltip
    ) {
        int scale = Math.round(1.0f / step);
        int minScaled = Math.round(min * scale);
        int maxScaled = Math.round(max * scale);
        int valueScaled = Math.round(Math.max(min, Math.min(max, value)) * scale);
        int defaultScaled = Math.round(Math.max(min, Math.min(max, defaultValue)) * scale);
        return new DependentIntegerSliderEntry(
                label,
                valueScaled,
                minScaled,
                maxScaled,
                Component.empty(),
                () -> defaultScaled,
                v -> saveConsumer.accept(v / (float) scale),
                () -> Optional.of(tooltip),
                false,
                v -> Component.literal(String.format(Locale.ROOT, "%.2f", v / (float) scale)),
                enabledSupplier
        );
    }

    public static final class DependentIntegerSliderEntry extends IntegerSliderEntry {
        private final Supplier<Boolean> enabledSupplier;

        private DependentIntegerSliderEntry(
                Component fieldName,
                int value,
                int minimum,
                int maximum,
                Component resetButtonKey,
                Supplier<Integer> defaultValue,
                Consumer<Integer> saveConsumer,
                Supplier<Optional<Component[]>> tooltipSupplier,
                boolean requiresRestart,
                java.util.function.Function<Integer, Component> textGetter,
                Supplier<Boolean> enabledSupplier
        ) {
            super(fieldName, minimum, maximum, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, requiresRestart);
            this.enabledSupplier = enabledSupplier;
            setTextGetter(textGetter);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            setEditable(enabledSupplier.get());
            super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        }
    }
    public static Component moduleToggleTitle(String key) {
        return Component.translatable(switch (key) {
            case "enableBlockHeavySources" -> "nourished.config.enableBlockHeavySources";
            case "enableBlockLightSource" -> "nourished.config.enableBlockLightSource";
            case "enableRawFoodPenalty" -> "config.nourished.enableRawFoodPenalty";
            case "enableGutHealth" -> "config.nourished.enableGutHealth";
            case "enableSynergies" -> "config.nourished.enableSynergies";
            case "enableMilestones" -> "config.nourished.enableMilestones";
            case "enableSeasonHooks" -> "config.nourished.enableSeasonHooks";
            case "enableAbsorptionModifiers" -> "config.nourished.enableAbsorptionModifiers";
            default -> "config.nourished." + key;
        });
    }

    public static Component moduleToggleDescription(String key) {
        return Component.translatable(switch (key) {
            case "enableBlockHeavySources" -> "nourished.config.enableBlockHeavySources.desc";
            case "enableBlockLightSource" -> "nourished.config.enableBlockLightSource.desc";
            case "enableRawFoodPenalty" -> "config.nourished.enableRawFoodPenalty.desc";
            case "enableGutHealth" -> "config.nourished.enableGutHealth.desc";
            case "enableSynergies" -> "config.nourished.enableSynergies.desc";
            case "enableMilestones" -> "config.nourished.enableMilestones.desc";
            case "enableSeasonHooks" -> "config.nourished.enableSeasonHooks.desc";
            case "enableAbsorptionModifiers" -> "config.nourished.enableAbsorptionModifiers.desc";
            default -> "config.nourished." + key + ".desc";
        });
    }

    public static final class ReloadConfigsListEntry extends TooltipListEntry<Object> {
        private static final int BUTTON_WIDTH = 150;
        private static final int BUTTON_HEIGHT = 20;
        private static final int OPEN_DATAPACK_FOLDER_WIDTH = 140;
        private static final int RELOAD_ROW_GAP = 6;

        private final Button button;
        @Nullable private final Button openDatapackFolderButton;
        private final boolean includeOpenDatapackFolder;

        ReloadConfigsListEntry(boolean includeOpenDatapackFolder) {
            super(
                    Component.translatable("config.nourished.reloadConfigs"),
                    () -> Optional.of(new Component[]{Component.translatable("config.nourished.reloadConfigs.desc")}),
                    false);
            this.includeOpenDatapackFolder = includeOpenDatapackFolder;
            if (includeOpenDatapackFolder) {
                this.openDatapackFolderButton = Button.builder(
                                Component.literal("\uD83D\uDCC2 Open Datapack Folder"),
                                b -> NourishedConfigSharedWidgets.openCompatDatapackFolder())
                        .bounds(0, 0, OPEN_DATAPACK_FOLDER_WIDTH, BUTTON_HEIGHT)
                        .build();
            } else {
                this.openDatapackFolderButton = null;
            }
            this.button = Button.builder(Component.translatable("config.nourished.reloadConfigs"), b -> {
                        NourishedReloadHelper.reloadAll();
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
            return includeOpenDatapackFolder ? 50 : 24;
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
            int innerW = Math.max(0, entryWidth - 8);
            int sx = x + 4;
            if (openDatapackFolderButton != null) {
                openDatapackFolderButton.active = isEditable();
                layoutDualButtons(
                        openDatapackFolderButton,
                        button,
                        sx,
                        y,
                        innerW,
                        OPEN_DATAPACK_FOLDER_WIDTH + RELOAD_ROW_GAP + BUTTON_WIDTH,
                        RELOAD_ROW_GAP,
                        BUTTON_HEIGHT
                );
                openDatapackFolderButton.render(graphics, mouseX, mouseY, delta);
            } else {
                button.setY(y);
                button.setX(sx + Math.max(0, innerW - Math.min(BUTTON_WIDTH, innerW)));
                button.setWidth(Math.min(BUTTON_WIDTH, innerW));
            }
            button.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            if (openDatapackFolderButton != null) {
                return List.of(openDatapackFolderButton, button);
            }
            return List.of(button);
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            if (openDatapackFolderButton != null) {
                return List.of(openDatapackFolderButton, button);
            }
            return List.of(button);
        }
    }
}
