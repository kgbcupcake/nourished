package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.SUMMARY_BOX_LOCAL_WIDTH;

/** Independent Calories summary box module — same self-positioning/collapse/drag-resize pattern as {@link RecentMealsComponent}/{@link EatMoreComponent}/{@link ActiveEffectsComponent}. */
public final class CaloriesComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.calories";
    private static final int HEADER_LOCAL_HEIGHT = 20;
    private static final int BODY_LOCAL_HEIGHT = 20;
    private static final int BASE_BOX_LOCAL_HEIGHT = HEADER_LOCAL_HEIGHT + BODY_LOCAL_HEIGHT;

    /** Reference local-unit padding used to derive the user's padding-adjustment range — see {@link ContentScaleController#resolvePadding}. */
    private static final double BASE_PADDING_LOCAL = 2.0d;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final TrackingData data;
    private final boolean visible;
    private final int renderedContentHeight;
    private final int localHeight;
    private final int boxLocalHeight;
    private final Bounds resolvedBounds;
    private final SummaryBoxRenderSupport support;
    private double contentScale = 1.0d;

    CaloriesComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;
        this.support = new SummaryBoxRenderSupport(startLocalY);

        NourishedClientConfig cc = NourishedClientConfig.get();
        Minecraft mc = Minecraft.getInstance();
        this.data = mc.player != null ? MarieClientCache.get() : null;
        this.boxLocalHeight = BASE_BOX_LOCAL_HEIGHT;

        // Continuous fade instead of an all-or-nothing header floor: header and bar scale down
        // together as room tightens (see render()'s heightScale, divided by the fixed boxLocalHeight
        // rather than this frame's shrunk room) — the box only actually disappears once there's less
        // than MIN_VISIBLE_ROOM_LOCAL of room left, instead of vanishing the instant its full natural
        // height stops fitting.
        boolean enabled = FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox() && data != null;
        int room = enabled ? DietLayout.roomInPanel(layout, startLocalY, boxLocalHeight) : 0;
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;
        this.renderedContentHeight = visible ? room : 0;
        this.localHeight = visible ? renderedContentHeight + DietScreenModules.MODULE_GAP_LOCAL : 0;

        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, SUMMARY_BOX_LOCAL_WIDTH, localHeight);
    }

    /** Local (pre-scale) pixel height this box occupies this frame; 0 when hidden or not fitting. */
    @Override
    public int localHeight() {
        return localHeight;
    }

    /** The box's content-driven height in local units, regardless of {@code visible} — see {@link RecentMealsComponent#naturalLocalHeight()}. */
    public int naturalLocalHeight() {
        return boxLocalHeight + DietScreenModules.MODULE_GAP_LOCAL;
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
        NourishedClientConfig cc = NourishedClientConfig.get();

        // min() of both ratios so a single-axis resize can't hide content. Divides by the fixed
        // boxLocalHeight (the full natural header+bar extent), not this frame's shrunk
        // renderedContentHeight — so header and bar scale down together continuously as room
        // tightens, instead of the header staying full-size right up until it's clipped off.
        double widthScale = bounds.width() / (double) SUMMARY_BOX_LOCAL_WIDTH;
        double heightScale = bounds.height() / (double) boxLocalHeight;
        this.contentScale = Math.min(widthScale, heightScale);
        // contentScale (fitScale) still drives sx/sy/sd/outer-box sizing unchanged; the persisted
        // per-box zoom multiplier only affects the size passed to text/icon draw calls below, and is
        // clamped to a flat [fitScale*0.5, fitScale*3.0] range (see zoomedTextIconScale's javadoc) —
        // real containment against the box's own edges comes from this box's own pushClip, not from
        // this range.
        float scale = ContentScaleController.resolveContentScale(contentScale, DietScreenPersistence.contentScale(ID));
        double paddingLocal = ContentScaleController.resolvePadding(BASE_PADDING_LOCAL, DietScreenPersistence.paddingScale(ID)) - BASE_PADDING_LOCAL;
        support.begin(bounds, contentScale, paddingLocal);

        support.drawOuterBox(context, bounds.width(), bounds.height(), cc);

        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            support.drawItem(context, "minecraft:fire_charge", 2, 5, scale);
            support.drawText(context, Component.translatable("nourished.screen.diet.calories_label").getString(), 24, 6, SummaryBoxRenderSupport.COL_WHITE, scale);

            String calStr = (int) data.total + " / " + (int) data.maxTotal;
            support.drawText(context, calStr, 24, 17, SummaryBoxRenderSupport.COL_GREEN, scale);

            int barLocalWidth = SUMMARY_BOX_LOCAL_WIDTH - 4;
            float calPct = data.maxTotal > 0 ? Mth.clamp(data.total / data.maxTotal, 0f, 1f) : 0f;
            context.drawBar(support.sx(2), support.sy(startLocalY + 33), support.sd(barLocalWidth), support.sd(4), calPct, SummaryBoxRenderSupport.COL_SEG_EMPTY, SummaryBoxRenderSupport.COL_GREEN);
        } finally {
            context.popClip();
        }
    }
}
