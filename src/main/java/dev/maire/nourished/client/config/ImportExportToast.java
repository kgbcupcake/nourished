package dev.maire.nourished.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

/**
 * Short single-line toast for config import/export feedback.
 */
public final class ImportExportToast implements Toast {

    private static final long DISPLAY_MS = 4500L;
    private final Component line;

    public ImportExportToast(Component line) {
        this.line = line;
    }

    public static void show(Component line) {
        Minecraft.getInstance().getToasts().addToast(new ImportExportToast(line));
    }

    @Override
    public int width() {
        return Math.min(380, Math.max(180, Minecraft.getInstance().font.width(line) + 20));
    }

    @Override
    public int height() {
        return 32;
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        var font = toastComponent.getMinecraft().font;
        guiGraphics.fill(0, 0, width(), height(), 0xF0100010);
        guiGraphics.renderOutline(0, 0, width(), height(), 0xFF505078);
        guiGraphics.drawString(font, line, 8, 12, 0xFFFFFF, false);
        return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}
