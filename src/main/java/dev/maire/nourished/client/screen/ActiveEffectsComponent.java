package dev.maire.nourished.client.screen;

import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.Constraint;
import dev.marie.framework.ui.HeaderCollapsibleComponent;
import dev.marie.framework.ui.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

/**
 * Independently draggable/resizable port of the "Active Effects" block that used to live inline in
 * {@link DietLeftColumnComponent#render}, following the same pattern as {@link RecentMealsComponent}/
 * {@link EatMoreComponent}. Previously this was plain text positioned wherever the stacking math
 * after RecentMeals/EatMore landed — meaning resizing either of those boxes moved it, and growing
 * them enough overlapped it with no way to fix that short of shrinking them back down. Making it a
 * real module with its own persisted {@link Bounds} lets the player drag it out of the way instead.
 */
final class ActiveEffectsComponent implements MarieComponent, HeaderCollapsibleComponent {

    static final String ID = "nourished.diet.activeeffects";
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
        int lineCount = Math.max(1, Math.min(3, effectCount));
        this.effectsBoxH = HEADER_LOCAL_HEIGHT + lineCount * 9;
        // Revisits the earlier "clampToPanel already guarantees containment, nothing left to
        // prevent" reasoning: containment within the panel rectangle isn't the same as not
        // overlapping RecentMeals/EatMore above it or getting cut off — a panel shrunk shorter via
        // its top/bottom edge left this section always "visible" and got clamped/slid to fit the
        // live rectangle with no regard for whether it still fit its stacked position at all. Now
        // live-height-aware like RecentMealsComponent/EatMoreComponent, so it collapses cleanly
        // instead. See RecentMealsComponent's constructor javadoc for why panelH()/scale() is used.
        int liveLocalHeight = (int) Math.round(layout.panelH() / layout.scale());
        int maxY = liveLocalHeight - DietLayout.PAD;
        this.visible = cc.showActiveEffects() && mc.player != null && (startLocalY + effectsBoxH <= maxY);
        this.localHeight = visible ? effectsBoxH + 8 : 0;

        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, bw, localHeight);
    }

    /** Local (pre-scale) pixel height this section occupies this frame; 0 when hidden or not fitting. */
    int localHeight() {
        return localHeight;
    }

    /**
     * The box's content-driven height in local units, regardless of {@code visible} — see {@link
     * RecentMealsComponent#naturalLocalHeight()}'s javadoc for why resize-clamp reference sizes must
     * use this instead of {@link #localHeight()}.
     */
    int naturalLocalHeight() {
        return effectsBoxH + 8;
    }

    /**
     * This section's current screen {@link Bounds} — persisted (once drag/resize lands) or, absent
     * saved state, today's default stacked position. Resolved once at construction, not recomputed
     * every frame, since a Layout recomputing this on every {@code render()} call would silently
     * override any future drag/resize commit on the very next frame.
     */
    Bounds resolvedBounds() {
        return resolvedBounds;
    }

    /** Whether this section fits at all (config-enabled, has a player, and its stacked position fits the live panel height) — see constructor. */
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int x = DietLayout.PAD;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        this.contentScale = bounds.width() / (double) bw;
        float scale = (float) contentScale;
        int y = startLocalY;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();

        NourishedClientConfig cc = NourishedClientConfig.get();
        drawOuterBox(context, bounds.width(), bounds.height(), cc);
        // Cosmetic: slightly smaller than body text and nudged down a couple local units, purely a
        // visual tweak — HEADER_LOCAL_HEIGHT (the layout reservation below) is untouched.
        drawText(context, Component.translatable("nourished.screen.diet.effects_label").getString(), x, y + 2, COL_HEADER, scale * 0.9f);
        y += HEADER_LOCAL_HEIGHT;

        if (effects.isEmpty()) {
            return;
        }

        // Line count reflows with the box's live (possibly edge-resized) height instead of shrinking
        // text to force everything to fit — shared HeaderCollapsibleComponent#bodyUnitsFit contract,
        // same collapse-on-shrink behavior as RecentMealsComponent's rows. Reserves the same trailing
        // 8-local-unit bottom pad naturalLocalHeight()/localHeight already budget for this box
        // (effectsBoxH + 8) — see EatMoreComponent's render() for the same fix and why omitting this
        // pad here would make this box's fit threshold inconsistent with its own natural-size formula.
        int bottomPadScreenH = (int) Math.round(8 * contentScale);
        Bounds bodyBounds = new Bounds(bounds.x(), bounds.y(), bounds.width(), Math.max(0, bounds.height() - bottomPadScreenH));
        int linesToShow = bodyUnitsFit(bodyBounds, contentScale, effects.size(), 9);

        int count = 0;
        for (MobEffectInstance effect : effects) {
            if (count >= linesToShow) break;
            MobEffect type = effect.getEffect().value();
            String name = Component.translatable(type.getDescriptionId()).getString();
            int amplifier = effect.getAmplifier();
            String label = (amplifier > 0 ? name + " " + (amplifier + 1) : name);
            int color = type.isBeneficial() ? COL_GREEN : COL_RED;
            String prefix = type.isBeneficial() ? "+ " : "- ";
            drawText(context, prefix + label, x, y, color, scale);
            y += 9;
            count++;
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
