package dev.maire.nourished.client.screen;

import dev.maire.nourished.client.NourishedKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent overlay screen that captures all input while Diet screen edit mode is active.
 * isPauseScreen=false keeps the world ticking. The diet panel and edit overlay are drawn by
 * {@link DietScreen#renderForEditScreen}; this screen adds short on-screen hints only.
 */
public final class DietScreenEditScreen extends Screen {

    private static final int HINT_COLOR = 0xFFFFFFFF;

    public DietScreenEditScreen() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Suppress the default dirt/blur background — screen must be fully transparent. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // intentionally empty
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        DietScreen.renderForEditScreen(graphics, minecraft);
        drawEditHints(graphics);
    }

    private void drawEditHints(GuiGraphics g) {
        if (minecraft == null || minecraft.font == null) return;
        String line1 = "Drag the panel to reposition.";
        String line2 = "Drag the bottom-right corner handle to resize.";
        int y = 22;
        int x1 = (width - font.width(line1)) / 2;
        int x2 = (width - font.width(line2)) / 2;
        g.drawString(font, line1, x1, y, HINT_COLOR, false);
        g.drawString(font, line2, x2, y + 11, HINT_COLOR, false);
    }

    /** J (or Escape) exits edit mode; all other keys are consumed to freeze player movement. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.EDIT_DIET_SCREEN.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            DietScreenEditMode.setActive(false);
            return true;
        }
        return true; // consume everything else
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DietScreenEditController.onEditMousePress((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        DietScreenEditController.onEditMouseRelease((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true;
    }

    /** Called when Minecraft closes the screen externally (e.g. player death). */
    @Override
    public void onClose() {
        DietScreenEditMode.setActive(false);
    }
}
