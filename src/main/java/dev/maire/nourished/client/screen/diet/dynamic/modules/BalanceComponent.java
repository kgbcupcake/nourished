package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.SUMMARY_BOX_LOCAL_WIDTH;

/** Independent Balance summary box module — same self-positioning/collapse/drag-resize pattern as {@link CaloriesComponent}/{@link RecentMealsComponent}/{@link EatMoreComponent}/{@link ActiveEffectsComponent}. */
public final class BalanceComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.balance";
    private static final int HEADER_LOCAL_HEIGHT = 30;
    private static final int BODY_LOCAL_HEIGHT = 20;
    private static final int BOX_LOCAL_HEIGHT = HEADER_LOCAL_HEIGHT + BODY_LOCAL_HEIGHT;

    private static final int COL_ORANGE = 0xFFFFAA00;
    private static final int COL_RED = 0xFFFF5555;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final TrackingData data;
    private final boolean visible;
    private final int renderedContentHeight;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private final SummaryBoxRenderSupport support;
    private double contentScale = 1.0d;

    BalanceComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;
        this.support = new SummaryBoxRenderSupport(startLocalY);

        NourishedClientConfig cc = NourishedClientConfig.get();
        Minecraft mc = Minecraft.getInstance();
        this.data = mc.player != null ? MarieClientCache.get() : null;

        // Continuous fade instead of an all-or-nothing header floor — see CaloriesComponent's
        // constructor comment; same pattern, header+pips scale down together as room tightens.
        boolean enabled = cc.showBalanceBox() && data != null;
        int room = enabled ? DietLayout.roomInPanel(layout, startLocalY, BOX_LOCAL_HEIGHT) : 0;
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;
        this.renderedContentHeight = visible ? room : 0;
        this.localHeight = visible ? renderedContentHeight + DietScreenModules.MODULE_GAP_LOCAL : 0;

        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, SUMMARY_BOX_LOCAL_WIDTH, localHeight);
    }

    @Override
    public int localHeight() {
        return localHeight;
    }

    public int naturalLocalHeight() {
        return BOX_LOCAL_HEIGHT + DietScreenModules.MODULE_GAP_LOCAL;
    }

    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

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
        return Constraint.preferred(DietLayout.toScreenDim(layout, SUMMARY_BOX_LOCAL_WIDTH), DietLayout.toScreenDim(layout, localHeight));
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        if (!visible) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        NourishedClientConfig cc = NourishedClientConfig.get();

        // min() of both ratios so a single-axis resize can't hide content. Divides by the fixed
        // BOX_LOCAL_HEIGHT (the full natural header+pips extent), not this frame's shrunk
        // renderedContentHeight — so header and pips scale down together continuously as room
        // tightens, instead of the header staying full-size right up until it's clipped off.
        double widthScale = bounds.width() / (double) SUMMARY_BOX_LOCAL_WIDTH;
        double heightScale = bounds.height() / (double) BOX_LOCAL_HEIGHT;
        this.contentScale = Math.min(widthScale, heightScale);
        // contentScale (fitScale) still drives sx/sy/sd/outer-box sizing unchanged; the persisted
        // per-box zoom multiplier only affects the size passed to text/icon draw calls below, and is
        // clamped to a flat [fitScale*0.5, fitScale*3.0] range (see zoomedTextIconScale's javadoc) —
        // real containment against the box's own edges comes from this box's own pushClip, not from
        // this range.
        float scale = DietScreenModules.zoomedTextIconScale(contentScale, widthScale, heightScale, DietScreenPersistence.contentScale(ID));
        support.begin(bounds, contentScale);

        support.drawOuterBox(context, bounds.width(), bounds.height(), cc);

        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            support.drawItem(context, "minecraft:comparator", 2, 5, scale);
            support.drawText(context, Component.translatable("nourished.screen.diet.balance_label").getString(), 24, 6, SummaryBoxRenderSupport.COL_WHITE, scale);

            String balKey = getBalanceKey(data);
            int balColor = balanceColor(balKey);
            String balText = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();

            Font font = mc.font;
            float balanceScale = 1.2f * (10f / 9f);
            float balTextW = font.width(balText) * balanceScale;
            int bgAlpha = 51;
            int bgColor = (bgAlpha << 24) | (balColor & 0x00FFFFFF);
            context.fillRect(support.sx(22), support.sy(startLocalY + 16), support.sd((int) balTextW + 5), support.sd(11), bgColor);
            support.drawText(context, balText, 24, 17, balColor, scale * balanceScale);

            int barLocalWidth = SUMMARY_BOX_LOCAL_WIDTH - 4;
            float balScore = MarieClientCache.getBalanceScore();
            int filledPips = Math.round(balScore * 5);
            int pipTotalW = 5 * 10 + 4 * 3;
            int pipStartX = (barLocalWidth - pipTotalW) / 2 + 2;
            for (int i = 0; i < 5; i++) {
                int px = pipStartX + i * 13;
                context.fillRect(support.sx(px), support.sy(startLocalY + 40), support.sd(10), support.sd(6), i < filledPips ? balColor : SummaryBoxRenderSupport.COL_SEG_EMPTY);
            }
        } finally {
            context.popClip();
        }
    }

    private static String getBalanceKey(TrackingData data) {
        NourishedConfig config = NourishedConfig.get();
        float critical = (float) config.criticalThreshold();
        float excessThreshold = (float) config.excessThreshold();
        boolean low = data.values.values().stream().anyMatch(v -> v < critical);
        boolean excess = data.values.values().stream().anyMatch(v -> v > excessThreshold);
        if (low) return "low";
        if (excess) return "excess";
        return "balanced";
    }

    private static int balanceColor(String key) {
        return switch (key) {
            case "balanced" -> SummaryBoxRenderSupport.COL_GREEN;
            case "low" -> COL_ORANGE;
            case "excess" -> COL_RED;
            default -> SummaryBoxRenderSupport.COL_WHITE;
        };
    }
}
