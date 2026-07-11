package dev.maire.nourished.client.screen;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.client.MarieValueColors;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
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
final class RecentMealsComponent implements MarieComponent, HeaderCollapsibleComponent {

    private static final int HEADER_LOCAL_HEIGHT = 10;

    static final String ID = "nourished.diet.recentmeals";

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_HEADER = 0xFF888888;
    private static final int COL_WHITE = 0xFFFFFFFF;

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
        this.localHeight = visible ? recentHeight + 8 : 0;

        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, bw, localHeight);
    }

    /** Local (pre-scale) pixel height this section occupies this frame; 0 when hidden or not fitting. */
    int localHeight() {
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
    int naturalLocalHeight() {
        return recentHeight + 8;
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

    /** Whether this section fits at all (config-enabled, has data, and its stacked position fits the live panel height) — see constructor. */
    boolean isVisible() {
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
        Font font = Minecraft.getInstance().font;
        NourishedClientConfig cc = NourishedClientConfig.get();

        int x = DietLayout.PAD;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        this.contentScale = bounds.width() / (double) bw;
        float scale = (float) contentScale;
        double recentMealsScale = layout.recentMealsScale();
        int rowH = Math.max(1, (int) Math.round(9 * recentMealsScale));
        int y = startLocalY;

        drawOuterBox(context, bounds.width(), bounds.height(), cc);
        drawText(context, Component.translatable("nourished.screen.diet.recent_label").getString(), x, y, COL_HEADER, scale);
        y += HEADER_LOCAL_HEIGHT;

        // Row count reflows with the box's live (possibly edge-resized) height instead of a fixed
        // cap of 3 — shared HeaderCollapsibleComponent#bodyUnitsFit contract: rows are drawn at a
        // fixed contentScale-derived screen size (icon/text SIZE is untouched — see class javadoc),
        // so contentScale is also the correct "screen pixels per local unit" ratio for measuring how
        // many of those fixed-size rows fit in bounds.height(). This is not the same computation as
        // recovering a local height from a live size (the EatMoreComponent bug this was audited
        // against) — it never treats bounds.height() as if it were produced by contentScale; it only
        // asks how many contentScale-sized rows fit inside whatever bounds.height() independently
        // turned out to be. The extra 8-local-unit bottom pad (matching localHeight()'s own "+8",
        // and ActiveEffectsComponent's identical pad) isn't part of the shared contract, so it's
        // applied here by shrinking the bounds passed in.
        int bottomPadScreenH = (int) Math.round(8 * contentScale);
        Bounds bodyBounds = new Bounds(bounds.x(), bounds.y(), bounds.width(), Math.max(0, bounds.height() - bottomPadScreenH));
        int rowsToShow = bodyUnitsFit(bodyBounds, contentScale, recentIds.size(), rowH);

        int count = 0;
        // Icon must render no taller than rowH (the per-row Y increment below), or consecutive rows
        // visually overlap regardless of how correct the Y-stepping math is — drawItem's `scale` is a
        // raw multiplier on a 16px icon (see RenderContext#drawItem), so the icon's rendered height in
        // local units is 16 * iconScale; deriving iconScale from rowH/16 instead of an unrelated fixed
        // constant (previously 0.75, which renders a 12-unit-tall icon into a 9-unit row) guarantees
        // the icon's footprint fits the row it's drawn into.
        float iconScale = rowH / 16f;
        int nameOffset = (int) Math.round(16 * recentMealsScale);
        for (String id : recentIds) {
            if (count >= rowsToShow) break;
            count++;

            ResourceLocation itemId = ResourceLocation.tryParse(id);
            if (itemId == null) {
                continue;
            }
            ItemStack recent = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            String name = recent.getHoverName().getString();
            // Skip the whole row (icon + name) rather than truncating with "..." when the full name
            // doesn't fit — a cut-off name reads as broken UI, whereas hiding it matches this box's
            // existing collapse-on-shrink behavior (fewer rows draw as the box shrinks; this is the
            // same "hide rather than degrade" choice applied to width instead of height). The row
            // slot is still consumed (y still advances below) so remaining rows don't shift up to
            // fill the gap.
            if (font.width(name) > bw - nameOffset) {
                y += rowH;
                continue;
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
            drawText(context, name, x + nameOffset, y + 3, nameColor, scale);
            y += rowH;
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
