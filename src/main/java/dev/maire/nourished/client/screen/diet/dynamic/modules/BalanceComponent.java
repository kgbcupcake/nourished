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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Independent Balance summary box module — same self-positioning/collapse/drag-resize pattern as {@link CaloriesComponent}/{@link RecentMealsComponent}/{@link EatMoreComponent}/{@link ActiveEffectsComponent}. */
public final class BalanceComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.balance";
    private static final int HEADER_LOCAL_HEIGHT = 30;
    private static final int BODY_LOCAL_HEIGHT = 20;
    private static final int BOX_LOCAL_HEIGHT = HEADER_LOCAL_HEIGHT + BODY_LOCAL_HEIGHT;

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_ORANGE = 0xFFFFAA00;
    private static final int COL_RED = 0xFFFF5555;
    private static final int COL_SEG_EMPTY = 0xFF2A2A2A;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final TrackingData data;
    private final boolean visible;
    private final boolean bodyVisible;
    private final int renderedContentHeight;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private Bounds anchorBounds;
    private double contentScale = 1.0d;

    BalanceComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;

        NourishedClientConfig cc = NourishedClientConfig.get();
        Minecraft mc = Minecraft.getInstance();
        this.data = mc.player != null ? MarieClientCache.get() : null;

        // The pip row is one indivisible body block (not a repeating list) — the header (icon +
        // label + balance state text) is the true floor; short of room for the pips too, the box
        // shrinks to header-only instead of vanishing outright.
        boolean headerVisible = cc.showBalanceBox() && data != null && DietLayout.headerFitsInPanel(layout, startLocalY, HEADER_LOCAL_HEIGHT);
        this.visible = headerVisible;
        this.bodyVisible = headerVisible && DietLayout.bodyBlockFitsInPanel(layout, startLocalY, HEADER_LOCAL_HEIGHT, BODY_LOCAL_HEIGHT);
        this.renderedContentHeight = visible ? HEADER_LOCAL_HEIGHT + (bodyVisible ? BODY_LOCAL_HEIGHT : 0) : 0;
        this.localHeight = visible ? renderedContentHeight + DietScreenModules.MODULE_GAP_LOCAL : 0;

        int boxLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2 + 4;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, boxLocalWidth, localHeight);
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
        int boxLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2 + 4;
        return Constraint.preferred(DietLayout.toScreenDim(layout, boxLocalWidth), DietLayout.toScreenDim(layout, localHeight));
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        this.anchorBounds = bounds;
        if (!visible) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // min() of both ratios so a single-axis resize can't hide content. Divides by
        // renderedContentHeight (this frame's actual header[+pips] extent), not the full natural
        // BOX_LOCAL_HEIGHT — so a graceful header-only shrink doesn't also squash the header's own
        // text scale.
        int boxLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2 + 4;
        double widthScale = bounds.width() / (double) boxLocalWidth;
        double heightScale = bounds.height() / (double) Math.max(1, renderedContentHeight);
        this.contentScale = Math.min(widthScale, heightScale);
        float scale = (float) contentScale;

        NourishedClientConfig cc = NourishedClientConfig.get();
        drawOuterBox(context, bounds.width(), bounds.height(), cc);

        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            drawItem(context, "minecraft:comparator", 2, 5, scale);
            drawText(context, Component.translatable("nourished.screen.diet.balance_label").getString(), 24, 6, COL_WHITE, scale);

            String balKey = getBalanceKey(data);
            int balColor = balanceColor(balKey);
            String balText = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();

            Font font = mc.font;
            float balanceScale = 1.2f * (10f / 9f);
            float balTextW = font.width(balText) * balanceScale;
            int bgAlpha = 51;
            int bgColor = (bgAlpha << 24) | (balColor & 0x00FFFFFF);
            context.fillRect(sx(22), sy(startLocalY + 16), sd((int) balTextW + 5), sd(11), bgColor);
            drawText(context, balText, 24, 17, balColor, scale * balanceScale);

            if (!bodyVisible) {
                return;
            }
            int barLocalWidth = DietLayout.SPLIT - DietLayout.PAD * 2;
            float balScore = MarieClientCache.getBalanceScore();
            int filledPips = Math.round(balScore * 5);
            int pipTotalW = 5 * 10 + 4 * 3;
            int pipStartX = (barLocalWidth - pipTotalW) / 2 + 2;
            for (int i = 0; i < 5; i++) {
                int px = pipStartX + i * 13;
                context.fillRect(sx(px), sy(startLocalY + 40), sd(10), sd(6), i < filledPips ? balColor : COL_SEG_EMPTY);
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
            case "balanced" -> COL_GREEN;
            case "low" -> COL_ORANGE;
            case "excess" -> COL_RED;
            default -> COL_WHITE;
        };
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
