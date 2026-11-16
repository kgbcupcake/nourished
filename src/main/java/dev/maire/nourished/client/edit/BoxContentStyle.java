package dev.maire.nourished.client.edit;

import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.modulesettings.ModuleExtents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * A box's Move Text/Icons/Bars offsets and Hide toggles, for content drawn straight to
 * {@link GuiGraphics} instead of through {@link MarieModuleSettings#withDisplaySettings}. Every draw
 * also reports where it landed, so the editor can outline and drag that part.
 */
public final class BoxContentStyle {

    /** No offsets, nothing hidden, nothing recorded (e.g. the standalone nutrients screen). */
    public static final BoxContentStyle NONE = new BoxContentStyle(null, null, 0, 0, 0, 0, 0, 0, false, false, false);

    private final PersistenceProvider store;
    private final String id;
    private final int textDx;
    private final int textDy;
    private final int iconDx;
    private final int iconDy;
    private final int barDx;
    private final int barDy;
    private final boolean hideText;
    private final boolean hideIcons;
    private final boolean hideBars;

    private BoxContentStyle(PersistenceProvider store, String id, int textDx, int textDy, int iconDx, int iconDy,
                            int barDx, int barDy, boolean hideText, boolean hideIcons, boolean hideBars) {
        this.store = store;
        this.id = id;
        this.textDx = textDx;
        this.textDy = textDy;
        this.iconDx = iconDx;
        this.iconDy = iconDy;
        this.barDx = barDx;
        this.barDy = barDy;
        this.hideText = hideText;
        this.hideIcons = hideIcons;
        this.hideBars = hideBars;
    }

    /** Reads {@code id}'s settings; call once per frame, after the extents were reset for this render. */
    public static BoxContentStyle of(PersistenceProvider store, String id) {
        return new BoxContentStyle(store, id,
                MarieModuleSettings.textOffsetX(store, id), MarieModuleSettings.textOffsetY(store, id),
                MarieModuleSettings.iconOffsetX(store, id), MarieModuleSettings.iconOffsetY(store, id),
                MarieModuleSettings.barOffsetX(store, id), MarieModuleSettings.barOffsetY(store, id),
                MarieModuleSettings.isTextHidden(store, id), MarieModuleSettings.isIconsHidden(store, id),
                MarieModuleSettings.isBarsHidden(store, id));
    }

    public void text(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        if (hideText) return;
        int w = g.drawString(font, text, x + textDx, y + textDy, color) - (x + textDx);
        record(ModuleExtents.Kind.TEXT, x + textDx, y + textDy, w, font.lineHeight);
    }

    public void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
        text(g, font, Component.literal(text), x, y, color);
    }

    public void text(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        if (hideText) return;
        g.drawString(font, text, x + textDx, y + textDy, color);
        record(ModuleExtents.Kind.TEXT, x + textDx, y + textDy, font.width(text), font.lineHeight);
    }

    public void item(GuiGraphics g, ItemStack stack, int x, int y) {
        if (hideIcons || stack.isEmpty()) return;
        g.renderItem(stack, x + iconDx, y + iconDy);
        record(ModuleExtents.Kind.ICON, x + iconDx, y + iconDy, 16, 16);
    }

    /** One rectangle of a bar or chart. */
    public void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (hideBars) return;
        g.fill(x1 + barDx, y1 + barDy, x2 + barDx, y2 + barDy, color);
        record(ModuleExtents.Kind.BAR, Math.min(x1, x2) + barDx, Math.min(y1, y2) + barDy, Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public int iconX(int x) {
        return x + iconDx;
    }

    public int iconY(int y) {
        return y + iconDy;
    }

    private void record(ModuleExtents.Kind kind, int x, int y, int w, int h) {
        if (store != null) ModuleExtents.add(store, id, kind, x, y, w, h);
    }
}
