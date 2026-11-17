package dev.maire.nourished.client.micro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Standalone window around {@link NutrientDetailView}, for the keybind and the Diet Screen's Nutrients box. */
public final class NutrientDetailScreen extends Screen {

    private static final int W = 340;
    private static final int BG = 0xE0101418;
    private static final int BORDER = 0xFF3A4450;

    @Nullable
    private final Screen parent;
    private final NutrientDetailView view;
    private int lastHeight = 200;

    /** {@code selected}: 0 opens the Overview, n opens the n-th nutrient. */
    public NutrientDetailScreen(@Nullable Screen parent, int selected) {
        super(Component.translatable("nourished.screen.nutrients.title"));
        this.parent = parent;
        this.view = new NutrientDetailView(selected);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = (width - W) / 2;
        int top = Math.max(4, (height - lastHeight) / 2);
        g.fill(left - 1, top - 1, left + W + 1, top + lastHeight + 1, BORDER);
        g.fill(left, top, left + W, top + lastHeight, BG);
        g.drawString(font, title, left + 6, top + 5, NutrientDetailView.TEXT);
        int used = view.render(g, font, left + 6, top + 20, W - 12, mouseX, mouseY);
        lastHeight = used + 26;
        if (!view.hovered().isEmpty()) {
            g.renderTooltip(font, view.hovered(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return (button == 0 && view.mouseClicked(mouseX, mouseY)) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return view.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
