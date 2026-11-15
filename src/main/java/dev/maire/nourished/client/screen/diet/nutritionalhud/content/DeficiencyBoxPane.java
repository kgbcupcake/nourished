package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.color.ColorKey;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.deficiency.DeficiencyView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** The Deficiency tab's box: {@link DeficiencyView} in a styled, scrolling box. */
public final class DeficiencyBoxPane extends ScrollingBoxPane {

    public static final String ID = "nourished.diet.deficiency_tab";

    private final DeficiencyView view = new DeficiencyView();

    @Override
    public String id() {
        return ID;
    }

    @Override
    protected ColorKey borderColor() {
        return NourishedColors.DEFICIENCY_BORDER;
    }

    @Override
    protected int renderContent(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY) {
        return view.render(g, font, x, y, w);
    }
}
