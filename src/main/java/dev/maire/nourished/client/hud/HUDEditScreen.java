package dev.maire.nourished.client.hud;

import dev.maire.nourished.client.NourishedKeys;
import dev.marie.MariesLib.config.ModuleCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent overlay screen that captures all input while HUD edit mode is active.
 * isPauseScreen=false keeps the world ticking. The HUD and edit overlay are drawn by
 * {@link NourishedHUD#renderForEditScreen}; this screen adds short on-screen hints only.
 */
public final class HUDEditScreen extends Screen {

    private static final int HINT_COLOR = 0xFFBBBBBB;

    public HUDEditScreen() {
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
        if (!ModuleCache.enableHUD) {
            return;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        NourishedHUD.renderForEditScreen(graphics, minecraft);
        drawEditHints(graphics);
    }

    /** Short hints under the top banner from {@link NourishedHUD}. */
    private void drawEditHints(GuiGraphics g) {
        if (minecraft == null || minecraft.font == null) return;
        String line1 = "Drag the HUD panel to reposition.";
        String line2 = "Drag the bottom-right corner handle to resize.";
        int y = 22;
        int x1 = (width - font.width(line1)) / 2;
        int x2 = (width - font.width(line2)) / 2;
        g.drawString(font, line1, x1, y, HINT_COLOR, false);
        g.drawString(font, line2, x2, y + 11, HINT_COLOR, false);
    }

    /** H (or Escape) exits edit mode; all other keys are consumed to freeze player movement. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.EDIT_HUD.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            HUDEditMode.setActive(false);
            return true;
        }
        return true; // consume everything else
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        NourishedHUD.onEditMousePress((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        NourishedHUD.onEditMouseRelease((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true; // drag position is sampled live from mouseHandler in NourishedHUD
    }

    /** Called when Minecraft closes the screen externally (e.g. player death). */
    @Override
    public void onClose() {
        HUDEditMode.setActive(false);
    }
}
