package dev.maire.nourished.client.config;

import dev.maire.nourished.config.NourishedPresetRegistry;
import dev.maire.nourished.core.Nourished;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Saves the current Nourished config as a preset under {@code config/nourished/presets/}.
 */
public final class NourishedSavePresetScreen extends Screen {

    private final Screen returnTo;
    private final Screen reopenParent;
    private EditBox nameBox;
    private EditBox descriptionBox;
    private Button saveButton;
    private Button cancelButton;

    public NourishedSavePresetScreen(Screen returnTo, Screen reopenParent) {
        super(Component.translatable(configKey("presets.saveDialog.title")));
        this.returnTo = returnTo;
        this.reopenParent = reopenParent;
    }

    private static String configKey(String suffix) {
        return "config." + Nourished.MODID + "." + suffix;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 50;
        nameBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.translatable(configKey("presets.saveDialog.name")));
        nameBox.setMaxLength(64);
        nameBox.setHint(Component.translatable(configKey("presets.saveDialog.nameHint")));
        addRenderableWidget(nameBox);

        descriptionBox = new EditBox(this.font, cx - 150, y + 28, 300, 20, Component.translatable(configKey("presets.saveDialog.description")));
        descriptionBox.setMaxLength(256);
        descriptionBox.setHint(Component.translatable(configKey("presets.saveDialog.descriptionHint")));
        addRenderableWidget(descriptionBox);

        saveButton = addRenderableWidget(
                Button.builder(Component.translatable(configKey("presets.saveDialog.save")), b -> trySave())
                        .bounds(cx - 155, y + 58, 150, 20)
                        .build());
        cancelButton = addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), b -> Minecraft.getInstance().setScreen(returnTo))
                        .bounds(cx + 5, y + 58, 150, 20)
                        .build());

        setInitialFocus(nameBox);
    }

    private void trySave() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String author = mc.player != null ? mc.player.getGameProfile().getName() : "";
        String desc = descriptionBox.getValue().trim();
        try {
            NourishedPresetRegistry.saveUserPreset(name, desc, author, NourishedImportExport.presetValuesFromCurrentConfig());
        } catch (Exception e) {
            Nourished.LOGGER.warn("[NourishedSavePresetScreen] Failed to save preset", e);
            return;
        }
        mc.setScreen(NourishedConfigScreen.create(reopenParent));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, this.height / 2 - 72, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable(configKey("presets.saveDialog.name")), cx - 150, this.height / 2 - 64, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable(configKey("presets.saveDialog.description")), cx - 150, this.height / 2 - 36, 0xA0A0A0, false);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String name = nameBox != null ? nameBox.getValue() : "";
        String desc = descriptionBox != null ? descriptionBox.getValue() : "";
        super.resize(minecraft, width, height);
        nameBox.setValue(name);
        descriptionBox.setValue(desc);
    }
}
