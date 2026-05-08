package dev.maire.nourished.api;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Provides custom rendering logic for a nutrient's HUD representation.
 *
 * <p>Implement this interface to replace the default nutrient bar rendering
 * with a custom visual (icon, radial gauge, particle effect, etc.).</p>
 */
@ApiStatus.Experimental
public interface NutrientRenderer {

    /**
     * Renders this nutrient's visual representation at the given screen coordinates.
     *
     * @param graphics the current GUI graphics context for draw calls
     * @param x        the left x-coordinate in screen space where rendering should begin
     * @param y        the top y-coordinate in screen space where rendering should begin
     * @param level    the current nutrient level, normalized between 0.0 (depleted) and 1.0 (full)
     */
    void render(GuiGraphics graphics, int x, int y, float level);
}
