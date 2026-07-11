package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.config.FeatureFlagCache;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Independent Calories summary box module — same self-positioning/collapse/drag-resize pattern as {@link RecentMealsComponent}/{@link EatMoreComponent}/{@link ActiveEffectsComponent}. */
public final class CaloriesComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.calories";
    private static final int HEADER_LOCAL_HEIGHT = 20;
    private static final int BODY_LOCAL_HEIGHT = 20;
    private static final int BOX_LOCAL_HEIGHT = HEADER_LOCAL_HEIGHT + BODY_LOCAL_HEIGHT;

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_SEG_EMPTY = 0xFF2A2A2A;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final TrackingData data;
    private final boolean visible;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private Bounds anchorBounds;
    private double contentScale = 1.0d;

    CaloriesComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;

        NourishedClientConfig cc = NourishedClientConfig.get();
        Minecraft mc = Minecraft.getInstance();
        this.data = mc.player != null ? MarieClientCache.get() : null;

        int liveLocalHeight = (int) Math.round(layout.panelH() / layout.scale());
        int maxY = liveLocalHeight - DietLayout.PAD;
        this.visible = FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox() && data != null
                && (startLocalY + BOX_LOCAL_HEIGHT <= maxY);
        this.localHeight = visible ? BOX_LOCAL_HEIGHT + DietScreenModules.MODULE_GAP_LOCAL : 0;

        int boxLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2 + 4;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, boxLocalWidth, localHeight);
    }

    /** Local (pre-scale) pixel height this box occupies this frame; 0 when hidden or not fitting. */
    @Override
    public int localHeight() {
        return localHeight;
    }

    /** The box's content-driven height in local units, regardless of {@code visible} — see {@link RecentMealsComponent#naturalLocalHeight()}. */
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
        int boxLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2 + 4;
        return Constraint.preferred(DietLayout.toScreenDim(layout, boxLocalWidth), DietLayout.toScreenDim(layout, localHeight));
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        this.anchorBounds = bounds;
        if (!visible) {
            return;
        }
        // min() of both ratios so a single-axis resize can't hide content.
        int boxLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2 + 4;
        double widthScale = bounds.width() / (double) boxLocalWidth;
        double heightScale = bounds.height() / (double) BOX_LOCAL_HEIGHT;
        this.contentScale = Math.min(widthScale, heightScale);
        float scale = (float) contentScale;

        NourishedClientConfig cc = NourishedClientConfig.get();
        drawOuterBox(context, bounds.width(), bounds.height(), cc);

        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            drawItem(context, "minecraft:fire_charge", 2, 5, scale);
            drawText(context, Component.translatable("nourished.screen.diet.calories_label").getString(), 24, 6, COL_WHITE, scale);

            String calStr = (int) data.total + " / " + (int) data.maxTotal;
            drawText(context, calStr, 24, 17, COL_GREEN, scale);

            int barLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2;
            float calPct = data.maxTotal > 0 ? Mth.clamp(data.total / data.maxTotal, 0f, 1f) : 0f;
            context.drawBar(sx(2), sy(startLocalY + 33), sd(barLocalWidth), sd(4), calPct, COL_SEG_EMPTY, COL_GREEN);
        } finally {
            context.popClip();
        }
    }

    // ── Coordinate + drawing helpers ─────────────────────────────────────────

    private int sx(int localX) {
        return anchorBounds.x() + (int) Math.round(localX * contentScale);
    }

    private int sy(int localY) {
        return anchorBounds.y() + (int) Math.round((localY - startLocalY) * contentScale);
    }

    private int sd(int localDim) {
        return (int) Math.round(localDim * contentScale);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(startLocalY + localY), color, scale);
    }

    private void drawItem(RenderContext context, String itemId, int localX, int localY, float scale) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(Items.APPLE);
        context.drawItem(new ItemStack(item), sx(localX), sy(startLocalY + localY), scale);
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
