package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.color.MarieColors;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.client.config.state.MarieClientCache;
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

    private static int balanceBalancedColor() {
        return MarieColors.resolveColor(NourishedColors.BALANCE_BALANCED);
    }

    private static int balanceLowColor() {
        return MarieColors.resolveColor(NourishedColors.BALANCE_LOW);
    }
    private static int balanceExcessColor() {
        return MarieColors.resolveColor(NourishedColors.BALANCE_EXCESS);
    }
    private static int headerTextColor() {
        return MarieColors.resolveColor(NourishedColors.BALANCE_HEADER);
    }

    private static int borderColor() {
        return MarieColors.resolveColor(NourishedColors.BALANCE_BORDER);
    }

    /** Reference local-unit padding used to derive the user's padding-adjustment range — see {@link ContentScaleController#resolvePadding}. */
    private static final double BASE_PADDING_LOCAL = 2.0d;

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
    public void render(RenderContext baseContext, Bounds bounds) {
        // The module's own text/icon offsets, icon size and brightness (see MarieModuleSettings) apply to everything it draws.
        RenderContext context = MarieModuleSettings.withDisplaySettings(baseContext, DietScreenPersistence.get(), ID);
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
                // Content geometry is fixed, like the Activity Log's: it follows the panel's own scale, never this
        // box's size, so resizing the box only changes the box (extra room stays empty, less room is
        // clipped by the box's own clip). Text/icon sizes come from their sliders alone.
        this.contentScale = layout.scale();
        // contentScale (fitScale) still drives sx/sy/sd/outer-box sizing unchanged; text/icon render
        // scale is the user's persisted per-box adjustment alone now, sanity-clamped only — no longer
        // capped by contentScale. Real containment against the box's own edges comes from this box's
        // own pushClip below.
        float scale = ContentScaleController.resolveContentScale(DietScreenPersistence.contentScale(ID));
        // Extra local-unit inset delta from the user's padding adjustment — zero at the default
        // (unadjusted) paddingScale, so existing layouts render identically until the player scrolls
        // in padding mode. Never applied to sd()/outer-box sizing, only to sx()/sy() positions.
        double userPaddingLocal = BASE_PADDING_LOCAL * DietScreenPersistence.paddingScale(ID);
        double paddingLocal = ContentScaleController.resolvePadding(userPaddingLocal) - BASE_PADDING_LOCAL;
        support.begin(bounds, contentScale, paddingLocal);

        support.drawOuterBox(context, bounds.width(), bounds.height(), cc, borderColor());

        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            support.drawItem(context, "minecraft:comparator", 2, 5, scale);
            // Drawn through the display-settings context, like the Calories and Eat More headers, so Move Text/Move All
            // moves it together with the state word below instead of leaving it fixed in place.
            support.drawText(context, Component.translatable("nourished.screen.diet.balance_label").getString(),
                    24, 6, headerTextColor(), scale);

            String balKey = getBalanceKey(data);
            int balColor = balanceColor(balKey);
            String balText = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();

            float balanceScale = 1.2f * (10f / 9f);
            support.drawText(context, balText, 24, 17, balColor, scale * balanceScale);

            int barLocalWidth = SUMMARY_BOX_LOCAL_WIDTH - 4;
            float balScore = MarieClientCache.getBalanceScore();
            int filledPips = Math.round(balScore * 5);
            int pipTotalW = 5 * 10 + 4 * 3;
            int pipStartX = (barLocalWidth - pipTotalW) / 2 + 2;
            // The pips are this box's "bar": Bar size scales them (from the row's start) and Move Bars offsets them.
            var store = DietScreenPersistence.get();
            float barScale = ContentScaleController.resolveContentScale(MarieModuleSettings.barScale(store, ID));
            int barDx = MarieModuleSettings.barOffsetX(store, ID);
            int barDy = MarieModuleSettings.barOffsetY(store, ID);
            int pipW = Math.max(1, Math.round(support.sd(10) * barScale));
            int pipH = Math.max(1, Math.round(support.sd(6) * barScale));
            int pipY = support.sy(startLocalY + 40) + barDy;
            int firstPipX = support.sx(pipStartX) + barDx;
            int lastPipRight = firstPipX;
            for (int i = 0; i < 5; i++) {
                int px = support.sx(pipStartX) + Math.round(i * support.sd(13) * barScale) + barDx;
                context.fillRect(px, pipY, pipW, pipH, i < filledPips ? balColor : SummaryBoxRenderSupport.barTrackColor());
                lastPipRight = px + pipW;
            }
            // The pips have no drawBar/drawVerticalBar call of their own (they're plain fillRects), so nothing records
            // their extent for MariesLib's move-outline/offset machinery on its own; report it so "Move Bars"/"Move All"
            // outlines hug the pip row instead of falling back to the whole box.
            MarieModuleSettings.recordBarExtent(store, ID, firstPipX, pipY, Math.max(1, lastPipRight - firstPipX), pipH);
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
            case "balanced" -> balanceBalancedColor();
            case "low" -> balanceLowColor();
            case "excess" -> balanceExcessColor();
            default -> SummaryBoxRenderSupport.textColor();
        };
    }
}
