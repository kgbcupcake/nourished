package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared local-to-screen coordinate mapping and draw helpers for the two fixed-shape summary boxes
 * (Calories/Balance) — both draw a header icon+label plus a small body against the same
 * anchor/startLocalY/contentScale scheme, previously hand-duplicated identically in each class.
 * RecentMeals/EatMore/ActiveEffects intentionally do NOT share this — see their own classes.
 */
final class SummaryBoxRenderSupport {

    static final int COL_ROW_BG_RGB = 0x001E1E1E;
    static final int COL_BORDER_LT = 0xFF555555;
    static final int COL_WHITE = 0xFFFFFFFF;
    static final int COL_GREEN = 0xFF55FF55;
    static final int COL_SEG_EMPTY = 0xFF2A2A2A;

    private final int startLocalY;
    private Bounds anchorBounds;
    private double contentScale = 1.0d;

    SummaryBoxRenderSupport(int startLocalY) {
        this.startLocalY = startLocalY;
    }

    /** Call once at the top of render(), before any sx/sy/sd/drawText/drawItem/drawOuterBox use this frame. */
    void begin(Bounds anchorBounds, double contentScale) {
        this.anchorBounds = anchorBounds;
        this.contentScale = contentScale;
    }

    int sx(int localX) {
        return anchorBounds.x() + (int) Math.round(localX * contentScale);
    }

    int sy(int localY) {
        return anchorBounds.y() + (int) Math.round((localY - startLocalY) * contentScale);
    }

    int sd(int localDim) {
        return (int) Math.round(localDim * contentScale);
    }

    void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(startLocalY + localY), color, scale);
    }

    void drawItem(RenderContext context, String itemId, int localX, int localY, float scale) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(Items.APPLE);
        context.drawItem(new ItemStack(item), sx(localX), sy(startLocalY + localY), scale);
    }

    void drawOuterBox(RenderContext context, int screenW, int screenH, NourishedClientConfig cc) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, cc.dietBackgroundOpacity());
        context.drawRoundedRect(anchorBounds.x(), anchorBounds.y(), screenW, screenH, 1, fill, COL_BORDER_LT);
    }

    static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
