package dev.maire.nourished.client.screen.diet.dynamic.modules;

import java.util.Collection;

import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.geometry.Bounds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Independently draggable/resizable port of the "Active Effects" block that used to live inline in
 * {@link DietLeftColumnComponent#render}, following the same pattern as {@link RecentMealsComponent}/
 * {@link EatMoreComponent}. Previously this was plain text positioned wherever the stacking math
 * after RecentMeals/EatMore landed — meaning resizing either of those boxes moved it, and growing
 * them enough overlapped it with no way to fix that short of shrinking them back down. Making it a
 * real module with its own persisted {@link Bounds} lets the player drag it out of the way instead.
 */
public final class ActiveEffectsComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.activeeffects";
    private static final int HEADER_LOCAL_HEIGHT = 10;

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_HEADER = 0xFF888888;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_RED = 0xFFFF5555;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final boolean visible;
    private final int effectsBoxH;
    private final int linesShown;
    private final int renderedContentHeight;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private Bounds anchorBounds;
    private double contentScale = 1.0d;

    ActiveEffectsComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;

        NourishedClientConfig cc = NourishedClientConfig.get();
        Minecraft mc = Minecraft.getInstance();
        int effectCount = (mc.player != null) ? mc.player.getActiveEffects().size() : 0;
        int naturalLineCount = Math.max(1, Math.min(3, effectCount));
        this.effectsBoxH = HEADER_LOCAL_HEIGHT + naturalLineCount * 9;
        // Continuous fade instead of an all-or-nothing header floor, and instead of dropping whole
        // effect lines one at a time as room tightens (the old stackedBodyUnitsFit behavior): every
        // natural line still draws, just scaled down together with the header (see render()'s
        // heightScale, divided by the fixed effectsBoxH rather than this frame's shrunk room) — the
        // box only actually disappears once there's less than MIN_VISIBLE_ROOM_LOCAL of room left.
        boolean enabled = cc.showActiveEffects() && mc.player != null;
        int room = enabled ? DietLayout.roomInPanel(layout, startLocalY, effectsBoxH) : 0;
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;
        this.linesShown = naturalLineCount;
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
     * The box's content-driven height in local units, regardless of {@code visible} — see {@link
     * RecentMealsComponent#naturalLocalHeight()}'s javadoc for why resize-clamp reference sizes must
     * use this instead of {@link #localHeight()}.
     */
    public int naturalLocalHeight() {
        return effectsBoxH + DietScreenModules.MODULE_GAP_LOCAL;
    }

    /**
     * This section's current screen {@link Bounds} — persisted (once drag/resize lands) or, absent
     * saved state, today's default stacked position. Resolved once at construction, not recomputed
     * every frame, since a Layout recomputing this on every {@code render()} call would silently
     * override any future drag/resize commit on the very next frame.
     */
    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

    /** Whether this section fits at all (config-enabled, has a player, and its stacked position fits the live panel height) — see constructor. */
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int x = DietLayout.PAD;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        int y = startLocalY;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        NourishedClientConfig cc = NourishedClientConfig.get();

        // min() of both ratios so a single-axis resize can't hide content. Divides by the fixed
        // effectsBoxH (the full natural header+lines extent), not this frame's shrunk
        // renderedContentHeight — so the header and every effect line scale down together
        // continuously as room tightens, instead of the header staying full-size right up until
        // it's clipped off.
        double widthScale = bounds.width() / (double) bw;
        double heightScale = bounds.height() / (double) effectsBoxH;
        this.contentScale = Math.min(widthScale, heightScale);
        // contentScale (fitScale) still drives sx/sy unchanged below; the persisted zoom multiplier
        // only affects the size passed to the header/line text draw calls. Clamped to a flat
        // [fitScale*0.5, fitScale*3.0] range (see zoomedTextIconScale's javadoc) — real containment
        // against the box's own edges comes from the pushClip(bounds...) below, not from this range.
        float scale = DietScreenModules.zoomedTextIconScale(contentScale, widthScale, heightScale, DietScreenPersistence.contentScale(ID));
        // Header-to-first-line and line-to-line advance must grow by the same ratio zoom grows text
        // draw size by — otherwise bigger zoomed lines visually collide into the next line's still-
        // unzoomed vertical slot, same bug just fixed in RecentMealsComponent (see its render() for
        // the full derivation). zoomRatio is exactly 1.0 whenever zoom is at/below fitScale (scale ==
        // contentScale), so this is a no-op at the default, unzoomed state. Purely cosmetic (keeps
        // lines from visually colliding) — pushClip below is what actually bounds on-screen overflow.
        double zoomRatio = contentScale > 0 ? scale / contentScale : 1.0d;
        int zoomedHeaderAdvance = Math.max(1, (int) Math.round(HEADER_LOCAL_HEIGHT * zoomRatio));
        int zoomedLineAdvance = Math.max(1, (int) Math.round(9 * zoomRatio));

        drawOuterBox(context, bounds.width(), bounds.height(), cc);

        int naturalLineCount = linesShown;
        // Clip wraps the header now too (not just the lines) — a continuously-shrinking box can be
        // shorter than even the header's own natural height, and without this the header text would
        // render past the box's actual (shrunk) bottom edge instead of fading out with it.
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            // Slightly smaller than body text, purely a visual tweak — HEADER_LOCAL_HEIGHT (the layout reservation below) is untouched.
            drawText(context, Component.translatable("nourished.screen.diet.effects_label").getString(), x, y + DietScreenModules.HEADER_TOP_PADDING_LOCAL, COL_HEADER, scale * 0.9f);
            y += zoomedHeaderAdvance;

            if (effects.isEmpty()) {
                return;
            }

            int count = 0;
            for (MobEffectInstance effect : effects) {
                if (count >= naturalLineCount) break;
                MobEffect type = effect.getEffect().value();
                String name = Component.translatable(type.getDescriptionId()).getString();
                int amplifier = effect.getAmplifier();
                String label = (amplifier > 0 ? name + " " + (amplifier + 1) : name);
                int color = type.isBeneficial() ? COL_GREEN : COL_RED;
                String prefix = type.isBeneficial() ? "+ " : "- ";
                drawText(context, prefix + label, x, y, color, scale);
                y += zoomedLineAdvance;
                count++;
            }
        } finally {
            context.popClip();
        }
    }

    // ── Coordinate + drawing helpers ─────────────────────────────────────────
    //
    // Same pattern as RecentMealsComponent/EatMoreComponent: the box's own screen origin comes from
    // the Bounds handed to render() (bounds.x()/bounds.y()), and local-to-screen scaling uses
    // `contentScale` (bounds.width() / this box's own reference width, `bw`), not the panel's
    // layout.scale() — so text sizing tracks this box's own independent size, and the drawn
    // rectangle exactly matches the Bounds DraggableResizable's resize handle/hit-testing use.

    private int sx(int localX) {
        return anchorBounds.x() + (int) Math.round(localX * contentScale);
    }

    private int sy(int localY) {
        return anchorBounds.y() + (int) Math.round((localY - startLocalY) * contentScale);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(localY), color, scale);
    }

    private void drawOuterBox(RenderContext context, int screenW, int screenH, NourishedClientConfig cc) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, cc.dietBackgroundOpacity());
        context.drawRoundedRect(anchorBounds.x(), anchorBounds.y(), screenW, screenH, 1, fill, COL_BORDER_LT);
    }

    private static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
