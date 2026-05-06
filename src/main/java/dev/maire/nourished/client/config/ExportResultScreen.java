package dev.maire.nourished.client.config;

import dev.maire.nourished.config.NourishedConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shows the {@code NCF1:} share code after a successful export.
 */
public final class ExportResultScreen extends Screen {

    private final Screen reopenParent;
    private final String relativePath;
    private final String shareCode;
    private EditBox shareField;

    public ExportResultScreen(Screen reopenParent, String relativePath, String shareCode) {
        super(Component.translatable("config.nourished.importExport.exportResultTitle"));
        this.reopenParent = reopenParent;
        this.relativePath = relativePath;
        this.shareCode = shareCode;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        shareField = new EditBox(this.font, cx - 180, y + 18, 360, 20, Component.translatable("config.nourished.importExport.shareCode"));
        shareField.setMaxLength(Math.max(shareCode.length() + 64, 524288));
        shareField.setValue(shareCode);
        shareField.setEditable(true);
        shareField.setFocused(true);
        addRenderableWidget(shareField);
        setInitialFocus(shareField);

        addRenderableWidget(
                Button.builder(Component.translatable("config.nourished.importExport.copyShareCode"), b -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(shareCode);
                            ImportExportToast.show(Component.translatable("config.nourished.importExport.copied"));
                        })
                        .bounds(cx - 180, y + 48, 175, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b ->
                                Minecraft.getInstance().setScreen(NourishedConfigScreen.create(reopenParent)))
                        .bounds(cx + 5, y + 48, 175, 20)
                        .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        graphics.drawCenteredString(this.font, this.title, cx, y - 28, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal(relativePath), cx, y - 12, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("config.nourished.importExport.shareCode"), cx - 180, y + 6, 0xA0A0A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
