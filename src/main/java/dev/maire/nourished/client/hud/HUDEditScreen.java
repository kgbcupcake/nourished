package dev.maire.nourished.client.hud;

import dev.maire.nourished.client.NourishedKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent overlay screen that captures all input while HUD edit mode is active.
 * isPauseScreen=false keeps the world ticking. The screen itself renders nothing —
 * NourishedHUD.renderForEditScreen() draws the HUD + edit overlay on top of the world.
 */
public final class HUDEditScreen extends Screen {

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
        super.render(graphics, mouseX, mouseY, partialTick);
        NourishedHUD.renderForEditScreen(graphics, minecraft);
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
