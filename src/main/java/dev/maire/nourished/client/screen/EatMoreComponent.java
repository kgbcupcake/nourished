package dev.maire.nourished.client.screen;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.Constraint;
import dev.marie.framework.ui.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.Nourished;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Static-rendering port of the "Eat more of..." block that used to live inline in
 * {@link DietLeftColumnComponent#render}, split out as its own {@link MarieComponent} in
 * preparation for independent drag/resize/persistence in a later pass. The drawing logic itself is
 * unchanged — only the class boundary moved. {@link #localHeight()} exposes the exact local
 * (pre-scale) pixel height this section consumes this frame (0 when hidden or not fitting), so
 * {@link DietLeftColumnComponent} can position whatever follows it without re-deriving this
 * section's fit/visibility logic.
 */
final class EatMoreComponent implements MarieComponent {

    static final String ID = "nourished.diet.eatmore";

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_HEADER = 0xFF888888;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final List<String> neglected;
    private final boolean visible;
    private final int eatBoxH;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private Bounds anchorBounds;

    EatMoreComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;
        this.neglected = MarieClientCache.getNeglectedCategories();

        NourishedClientConfig cc = NourishedClientConfig.get();
        int maxY = DietLayout.HEIGHT - DietLayout.PAD;
        this.eatBoxH = Math.max(1, (int) Math.round(46 * layout.eatMoreScale()));
        boolean showable = cc.showEatMoreOf() && !neglected.isEmpty();
        this.visible = showable && (startLocalY + eatBoxH <= maxY);
        this.localHeight = visible ? eatBoxH + 4 : 0;

        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, bw, localHeight);
    }

    /** Local (pre-scale) pixel height this section occupies this frame; 0 when hidden or not fitting. */
    int localHeight() {
        return localHeight;
    }

    /**
     * This section's current screen {@link Bounds} — persisted (once drag/resize lands) or, absent
     * saved state, today's default stacked position. Resolved once at construction, not
     * recomputed every frame, since a Layout recomputing this on every {@code render()} call would
     * silently override any future drag/resize commit on the very next frame.
     */
    Bounds resolvedBounds() {
        return resolvedBounds;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        return Constraint.preferred(DietLayout.toScreenDim(layout, bw), DietLayout.toScreenDim(layout, localHeight));
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        this.anchorBounds = bounds;
        if (!visible) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        float scale = (float) layout.scale();
        NourishedClientConfig cc = NourishedClientConfig.get();

        int x = DietLayout.PAD;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        int y = startLocalY;

        drawRoundedBox(context, x - 2, y - 2, bw + 4, eatBoxH, cc);
        String suggestionHeader = Component.translatable("nourished.screen.diet.suggestion_label").getString();
        drawText(context, font.plainSubstrByWidth(suggestionHeader, bw), x, y, COL_HEADER, scale);
        y += 10;

        for (int col = 0; col < Math.min(2, neglected.size()); col++) {
            String categoryKey = neglected.get(col);
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    ResourceLocation.parse(Nourished.MODID + ":nutrients/" + categoryKey));
            Item exampleItem = null;
            for (Item item : BuiltInRegistries.ITEM) {
                if (item.builtInRegistryHolder().is(tag)) {
                    exampleItem = item;
                    break;
                }
            }
            if (exampleItem == null) continue;

            int suggestionColW = (bw - 4) / 2;
            int colX = x + col * suggestionColW;
            context.drawItem(new ItemStack(exampleItem), sx(colX), sy(y), scale);
        }
    }

    // ── Coordinate + drawing helpers ─────────────────────────────────────────
    //
    // The box's own screen origin now comes from the Bounds handed to render() (bounds.x()/
    // bounds.y()) instead of being independently recomputed via DietLayout.toScreenX/Y — a
    // prerequisite for drag/resize, where DraggableResizable will hand this component a live
    // preview Bounds that must actually be honored. bounds.x() lines up with local X 0 (the
    // column's own left edge, unpadded — see DietLeftColumnComponent's childArea), so local X
    // offsets (still PAD-inclusive, e.g. "x - 2") pass straight through unchanged. bounds.y()
    // lines up with local Y == startLocalY (VerticalLayout stacks this component starting there),
    // so local Y values are rebased by subtracting startLocalY before scaling. Internal element
    // sizing (icon/row/box dimensions) is unaffected and still goes through
    // DietLayout.toScreenDim via {@link #sd}.

    private int sx(int localX) {
        return anchorBounds.x() + (int) Math.round(localX * layout.scale());
    }

    private int sy(int localY) {
        return anchorBounds.y() + (int) Math.round((localY - startLocalY) * layout.scale());
    }

    private int sd(int localDim) {
        return DietLayout.toScreenDim(layout, localDim);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(localY), color, scale);
    }

    private void drawRoundedBox(RenderContext context, int localX, int localY, int localW, int localH, NourishedClientConfig cc) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, cc.dietBackgroundOpacity());
        context.fillRect(sx(localX), sy(localY), sd(localW), sd(localH), fill);
        context.drawBorder(sx(localX), sy(localY), sd(localW), sd(localH), 1, COL_BORDER_LT);
    }

    private static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
