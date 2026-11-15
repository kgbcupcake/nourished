package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.color.ColorKey;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.micro.NutrientDetailView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** The Nutrients tab's box: {@link NutrientDetailView} in a styled, scrolling box. */
public final class NutrientsBoxPane extends ScrollingBoxPane {

    public static final String ID = "nourished.diet.nutrients_tab";

    private final NutrientDetailView view = new NutrientDetailView(0);

    @Override
    public String id() {
        return ID;
    }

    @Override
    protected ColorKey borderColor() {
        return NourishedColors.NUTRIENTS_BORDER;
    }

    @Override
    protected int renderContent(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY) {
        return view.render(g, font, x, y, w, mouseX, mouseY);
    }

    @Override
    protected boolean contentClicked(double mouseX, double mouseY) {
        return view.mouseClicked(mouseX, mouseY);
    }

    @Override
    protected boolean contentKey(int keyCode) {
        return view.keyPressed(keyCode);
    }
}
