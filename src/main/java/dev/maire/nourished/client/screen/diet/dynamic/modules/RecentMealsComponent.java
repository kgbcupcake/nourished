package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
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

    /** Reference local-unit padding used to derive the user's padding-adjustment range — see {@link ContentScaleController#resolvePadding}. */
    private static final double BASE_PADDING_LOCAL = 2.0d;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final List<String> recentIds;
    private final boolean visible;
    private final int recentHeight;
    private final int rowsShown;
    private final int renderedContentHeight;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private Bounds anchorBounds;
    private double contentScale = 1.0d;
    private double paddingLocal = 0.0d;

    RecentMealsComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;
        this.recentIds = MarieClientCache.getRecentSourceIds();

        NourishedClientConfig cc = NourishedClientConfig.get();
        // Per-item height (9) matches ActiveEffectsComponent's fixed line height exactly — both
        // boxes draw the same shape of content (icon + colored label, one row per item), so they
        // share one collapse-threshold constant rather than each having their own separately-tuned
        // value (this used to be 14 here, which made RecentMeals demand a much taller drag before any
        // row would show than ActiveEffects did for visually equivalent content). recentMealsScale is
        // a distinct, legitimate per-box scale (see NourishedClientConfig#recentMealsBoxScale) still
        // applied on top, same as it already scales the icon/name-offset below.
        int rowH = Math.max(1, (int) Math.round(9 * layout.recentMealsScale()));
        int naturalRows = Math.min(3, recentIds.size());
        this.recentHeight = HEADER_LOCAL_HEIGHT + (naturalRows * rowH);
        boolean showable = cc.showRecentMeals() && !recentIds.isEmpty();
        // Continuous fade instead of an all-or-nothing header floor, and instead of dropping whole
        // rows one at a time as room tightens (the old stackedBodyUnitsFit behavior): every natural
        // row still draws, just scaled down together with the header (see render()'s heightScale,
        // divided by the fixed recentHeight rather than this frame's shrunk room) — the box only
        // actually disappears once there's less than MIN_VISIBLE_ROOM_LOCAL of room left.
        int room = showable ? DietLayout.roomInPanel(layout, startLocalY, recentHeight) : 0;
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;
        this.rowsShown = naturalRows;
        this.renderedContentHeight = visible ? room : 0;
        this.localHeight = visible ? renderedContentHeight + DietScreenModules.MODULE_GAP_LOCAL : 0;

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

        // min() of both ratios so a single-axis resize can't hide content. Divides by the fixed
        // recentHeight (the full natural header+rows extent), not this frame's shrunk
        // renderedContentHeight — so the header and every row scale down together continuously as
        // room tightens, instead of the header staying full-size right up until it's clipped off.
        double widthScale = bounds.width() / (double) bw;
        double heightScale = bounds.height() / (double) recentHeight;
        this.contentScale = Math.min(widthScale, heightScale);
        // contentScale (fitScale) still drives sx/sy/availableLocalWidth unchanged below; the
        // persisted zoom multiplier only affects the size passed to text/icon draw calls (header,
        // icon, row name). zoomedTextIconScale now clamps to a flat [fitScale*0.5, fitScale*3.0]
        // range rather than deriving from widthScale/heightScale — see its javadoc. Whatever `scale`
        // comes back, the pushClip(bounds...) below is what actually keeps drawn content from
        // escaping the box; this clamp just keeps zoom in a usable range, not a containment guarantee.
        float scale = ContentScaleController.resolveContentScale(contentScale, DietScreenPersistence.contentScale(ID));
        this.paddingLocal = ContentScaleController.resolvePadding(BASE_PADDING_LOCAL, DietScreenPersistence.paddingScale(ID)) - BASE_PADDING_LOCAL;
        // Row spacing (and the header's own gap before the first row) must grow by the same ratio
        // zoom grows text/icon draw size by — otherwise the bigger zoomed glyphs visually collide
        // into the next row's still-unzoomed vertical slot. zoomRatio is exactly 1.0 whenever zoom
        // is at/below fitScale (scale == contentScale), so this is a no-op at the default,
        // unzoomed state. Applied to the LOCAL (pre-scale) Y advance fed into sy(), which then
        // reapplies contentScale — so the resulting on-screen spacing is contentScale * (rowH *
        // zoomRatio) == rowH * scale, matching the icon's own on-screen footprint (scale * iconScale
        // * 16 == scale * rowH) exactly, at any zoom level. This is purely cosmetic (keeps rows from
        // visually colliding with each other) — it does not bound on-screen overflow past the box's
        // own edges; pushClip below does that regardless of how large `scale` gets.
        double zoomRatio = contentScale > 0 ? scale / contentScale : 1.0d;
        int zoomedHeaderAdvance = Math.max(1, (int) Math.round(HEADER_LOCAL_HEIGHT * zoomRatio));
        int zoomedRowH = Math.max(1, (int) Math.round(rowH * zoomRatio));

        drawOuterBox(context, bounds.width(), bounds.height(), cc);

        int naturalRowCount = rowsShown;
        float iconScale = rowH / 16f; // fits the icon exactly within rowH — deliberately the flat, unzoomed rowH: this is a size ratio, not a position advance, and scale already carries the zoom
        float rowScale = scale * (float) recentMealsScale; // folds the recentMealsScale config knob into label size
        int nameOffset = rowH + 2;
        Font font = Minecraft.getInstance().font;
        // Cosmetic truncation budget, not a containment guarantee — actual containment is the
        // pushClip(bounds...) below, which hard-clips anything drawn oversized at the box's real
        // edge regardless of `rowScale`. Without this budget an oversized name would still get
        // scissor-clipped exactly at the box edge, just mid-glyph; truncating ahead of time turns
        // that into a clean "...". Recomputed from the box's actual LIVE bounds.width() every frame,
        // budgeted against the ACTUAL draw scale (rowScale) rather than contentScale — the name is
        // positioned via sx() (which maps local X through contentScale) but drawn via Font at
        // rowScale, and those two diverge once zoom pushes rowScale above contentScale.
        // font.width(name) returns the raw (scale-1) pixel width Font measures internally, so the
        // budget must be in that same raw unit — i.e. remaining screen pixels divided by rowScale.
        int nameOffsetScreenPx = (int) Math.round(nameOffset * contentScale);
        int availableNameScreenPx = bounds.width() - nameOffsetScreenPx;
        int maxNameFontPx = rowScale > 0f ? (int) Math.max(0, Math.floor(availableNameScreenPx / rowScale)) : 0;
        // Real containment: everything drawn below (header + rows) is hard-clipped to the box's own
        // live bounds via the GL scissor test, so nothing — however oversized `scale` makes it — can
        // ever paint outside this box, regardless of whether the truncation math above is exact. A
        // continuously-shrinking box can also be shorter than even the header's own natural height,
        // and without this the header text would render past the box's actual (shrunk) bottom edge
        // instead of fading out with it.
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            // Header gets the same width-aware truncation as row names below, for the same cosmetic
            // reason (clean "..." instead of a mid-glyph scissor cut) — it used to draw unconditionally
            // at `scale` with no width check at all.
            String header = Component.translatable("nourished.screen.diet.recent_label").getString();
            int headerOffsetScreenPx = (int) Math.round(x * contentScale);
            int availableHeaderScreenPx = bounds.width() - headerOffsetScreenPx;
            int maxHeaderFontPx = scale > 0f ? (int) Math.max(0, Math.floor(availableHeaderScreenPx / scale)) : 0;
            if (font.width(header) > maxHeaderFontPx) {
                int headerEllipsisW = font.width("...");
                int headerBudget = Math.max(0, maxHeaderFontPx - headerEllipsisW);
                header = font.plainSubstrByWidth(header, headerBudget) + "...";
            }
            drawText(context, header, x, y, COL_HEADER, scale);
            y += zoomedHeaderAdvance;
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
                        ? HudDrawHelpers.nutrientColorArgb(nutrientKey)
                        : COL_WHITE;
                drawText(context, name, x + nameOffset, y, nameColor, rowScale);
                y += zoomedRowH;
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
        return anchorBounds.x() + (int) Math.round((localX + paddingLocal) * contentScale);
    }

    private int sy(int localY) {
        return anchorBounds.y() + (int) Math.round((localY - startLocalY + paddingLocal) * contentScale);
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
