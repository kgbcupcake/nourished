package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.client.config.render.MarieValueColors;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Static-rendering port of the "Recent Meals" block that used to live inline in
 * {@link DietLeftColumnComponent#render}, split out as its own {@link MarieComponent} in
 * preparation for independent drag/resize/persistence in a later pass. The drawing logic itself is
 * unchanged — only the class boundary moved. {@link #localHeight()} exposes the exact local
 * (pre-scale) pixel height this section consumes this frame (0 when hidden or not fitting), so
 * {@link DietLeftColumnComponent} can position whatever follows it without re-deriving this
 * section's fit/visibility logic.
 */
public final class RecentMealsComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    private static final int HEADER_LOCAL_HEIGHT = 10;

    public static final String ID = "nourished.diet.recentmeals";

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_HEADER = 0xFF888888;
    private static final int COL_WHITE = 0xFFFFFFFF;

    /**
     * Hard character-count ceiling for a meal name, applied on top of the pixel-width truncation
     * check below — a backstop against font-metric/rounding edge cases the pixel check alone hasn't
     * reliably caught (e.g. "Cooked Llama Meat" still overflowing at typical sizes even with the
     * live-bounds pixel fix). Derived from the box's narrowest supported width, not its natural one:
     * {@code bw - nameOffset} (88 - 11 = 77 local units, itself scale-invariant under the two-axis
     * proportional content scale — see {@code contentScale}'s derivation above) at
     * {@code DietScreenEditTarget}'s 60% min-size floor gives ~46 local units of guaranteed available
     * width, divided by 6px — a deliberately generous per-character estimate for Minecraft's default
     * font (most glyphs are narrower; this errs conservative rather than truncating too early) — for
     * a floor of ~7-12 characters; rounded up slightly to 12 so short-but-real names ("Hamburger",
     * "Melon Slice") aren't clipped, while "Cooked Llama Meat" (18 chars) always is.
     */
    private static final int MAX_NAME_CHARS = 12;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final List<String> recentIds;
    private final boolean visible;
    private final int recentHeight;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private Bounds anchorBounds;
    private double contentScale = 1.0d;

    RecentMealsComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;
        this.recentIds = MarieClientCache.getRecentSourceIds();

        NourishedClientConfig cc = NourishedClientConfig.get();
        // Live-height-aware, not the fixed DietLayout.HEIGHT constant: the main panel's own height
        // can now be edge-resized independently of width (see DietScreenEditTarget#resolvedPanelLayout/
        // matchedLayoutFor), and layout.panelH() already reflects that live value — dividing by
        // layout.scale() recovers it in the same local-unit space startLocalY/recentHeight are
        // already expressed in, using the exact same width-derived scale every other local-Y-to-
        // screen conversion in this box already uses (not a "recovered aspect ratio" assumption).
        // Without this, a panel shrunk shorter via its top/bottom edge never hides this section even
        // though it no longer fits, so it just overlaps/cuts off instead of collapsing.
        int liveLocalHeight = (int) Math.round(layout.panelH() / layout.scale());
        int maxY = liveLocalHeight - DietLayout.PAD;
        // Per-item height (9) matches ActiveEffectsComponent's fixed line height exactly — both
        // boxes draw the same shape of content (icon + colored label, one row per item), so they
        // share one collapse-threshold constant rather than each having their own separately-tuned
        // value (this used to be 14 here, which made RecentMeals demand a much taller drag before any
        // row would show than ActiveEffects did for visually equivalent content). recentMealsScale is
        // a distinct, legitimate per-box scale (see NourishedClientConfig#recentMealsBoxScale) still
        // applied on top, same as it already scales the icon/name-offset below.
        int rowH = Math.max(1, (int) Math.round(9 * layout.recentMealsScale()));
        this.recentHeight = HEADER_LOCAL_HEIGHT + (Math.min(3, recentIds.size()) * rowH);
        boolean showable = cc.showRecentMeals() && !recentIds.isEmpty();
        this.visible = showable && (startLocalY + recentHeight <= maxY);
        // Bottom pad (8) matches ActiveEffectsComponent's bottom pad exactly — see bodyUnitsFit call
        // below and ActiveEffectsComponent#render for the identical pad-then-fit pattern.
        this.localHeight = visible ? recentHeight + DietScreenModules.MODULE_GAP_LOCAL : 0;

        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, bw, localHeight);
    }

    /** Local (pre-scale) pixel height this section occupies this frame; 0 when hidden or not fitting. */
    @Override
    public int localHeight() {
        return localHeight;
    }

    /**
     * The box's content-driven height in local units, regardless of {@code visible} — unlike {@link
     * #localHeight()}, never zeroed out just because this section isn't showing this particular
     * frame (empty recent-meals list, feature toggled off, doesn't currently fit). Resize-clamp
     * reference sizes must use this, not {@link #localHeight()}: a clamp rebuilt every frame (to
     * track live panel scale — see {@link DietScreenEditTarget#liveSubBoxConstraint}) from a
     * visibility-gated height can collapse to near-zero on any frame visibility happens to be false,
     * and if a resize commits on exactly that frame the persisted size gets stuck there.
     */
    public int naturalLocalHeight() {
        return recentHeight + DietScreenModules.MODULE_GAP_LOCAL;
    }

    /**
     * This section's current screen {@link Bounds} — persisted (once drag/resize lands) or, absent
     * saved state, today's default stacked position. Resolved once at construction, not
     * recomputed every frame, since a Layout recomputing this on every {@code render()} call would
     * silently override any future drag/resize commit on the very next frame.
     */
    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

    /** Whether this section fits at all (config-enabled, has data, and its stacked position fits the live panel height) — see constructor. */
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int headerLocalHeight() {
        return HEADER_LOCAL_HEIGHT;
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
        NourishedClientConfig cc = NourishedClientConfig.get();

        int x = DietLayout.PAD;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        double recentMealsScale = layout.recentMealsScale();
        int rowH = Math.max(1, (int) Math.round(9 * recentMealsScale));
        int y = startLocalY;

        // min() of both ratios so a single-axis resize can't hide content.
        double widthScale = bounds.width() / (double) bw;
        double heightScale = bounds.height() / (double) recentHeight;
        this.contentScale = Math.min(widthScale, heightScale);
        float scale = (float) contentScale;

        drawOuterBox(context, bounds.width(), bounds.height(), cc);
        drawText(context, Component.translatable("nourished.screen.diet.recent_label").getString(), x, y, COL_HEADER, scale);
        y += HEADER_LOCAL_HEIGHT;

        int naturalRowCount = Math.min(3, recentIds.size());
        float iconScale = rowH / 16f; // fits the icon exactly within rowH
        float rowScale = scale * (float) recentMealsScale; // folds the recentMealsScale config knob into label size
        int nameOffset = rowH + 2;
        Font font = Minecraft.getInstance().font;
        // Recomputed from the box's actual LIVE bounds.width()/contentScale every frame, not the
        // fixed natural-size `bw` — bw only equals bounds.width()/contentScale when width is the
        // binding axis of the two-axis content scale; when a height-only resize is the binding axis
        // instead, text renders smaller than what the box's real width would allow, and truncating
        // against fixed bw under-uses that extra room (or, before this fix, truncated nothing at all).
        int availableLocalWidth = (int) Math.round(bounds.width() / contentScale) - nameOffset;
        int maxNameFontPx = (int) Math.max(0, Math.round(availableLocalWidth / recentMealsScale));
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            int count = 0;
            for (String id : recentIds) {
                if (count >= naturalRowCount) break;
                count++;

                ResourceLocation itemId = ResourceLocation.tryParse(id);
                if (itemId == null) {
                    continue;
                }
                ItemStack recent = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
                String name = recent.getHoverName().getString();
                boolean overCharCap = name.length() > MAX_NAME_CHARS;
                if (overCharCap) {
                    name = name.substring(0, MAX_NAME_CHARS);
                }
                if (overCharCap || font.width(name) > maxNameFontPx) {
                    int ellipsisW = font.width("...");
                    int budget = Math.max(0, maxNameFontPx - ellipsisW);
                    name = font.plainSubstrByWidth(name, budget) + "...";
                }
                context.drawItem(recent, sx(x), sy(y), scale * iconScale);

                Map<String, Float> nutrientBars = NutrientClassificationLookup.resolveBars(recent.getItem());
                String nutrientKey = nutrientBars.entrySet().stream()
                        .max(Comparator.comparingDouble(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .orElse(null);
                int nameColor = nutrientKey != null
                        ? MarieValueColors.baseColorArgb(nutrientKey)
                        : COL_WHITE;
                drawText(context, name, x + nameOffset, y, nameColor, rowScale);
                y += rowH;
            }
        } finally {
            context.popClip();
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
    // so local Y values are rebased by subtracting startLocalY before scaling. Local-to-screen
    // scaling itself uses `contentScale` (bounds.width() / this box's own reference width, `bw`),
    // not the panel's layout.scale() — so icon/text/row sizing tracks this box's own independent
    // size instead of the main panel's, matching how the outer box already sizes off bounds
    // directly rather than through DietLayout.toScreenDim.

    private int sx(int localX) {
        return anchorBounds.x() + (int) Math.round(localX * contentScale);
    }

    private int sy(int localY) {
        return anchorBounds.y() + (int) Math.round((localY - startLocalY) * contentScale);
    }

    private int sd(int localDim) {
        return DietLayout.toScreenDim(layout, localDim);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(localY), color, scale);
    }

    private void drawRoundedBox(RenderContext context, int localX, int localY, int localW, int localH, NourishedClientConfig cc) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, cc.dietBackgroundOpacity());
        context.drawRoundedRect(sx(localX), sy(localY), sd(localW), sd(localH), 1, fill, COL_BORDER_LT);
    }

    /**
     * Outer box only: width/height are the box's own resolvedBounds() (or live drag preview) —
     * absolute pixels, not scaled via {@link #sd} against the panel's scale — so resizing the main
     * panel no longer also resizes this box. Position is the literal {@code anchorBounds.x()/y()}
     * corner (not routed through {@link #sx}/{@link #sy}, which are local-coordinate-relative and
     * offset from the box's own top-left by a few padding pixels) so the drawn rectangle exactly
     * matches the {@link Bounds} that {@link dev.marie.framework.ui.DraggableResizable}'s resize
     * handle and hit-testing are computed against.
     */
    private void drawOuterBox(RenderContext context, int screenW, int screenH, NourishedClientConfig cc) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, cc.dietBackgroundOpacity());
        context.drawRoundedRect(anchorBounds.x(), anchorBounds.y(), screenW, screenH, 1, fill, COL_BORDER_LT);
    }

    private static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
