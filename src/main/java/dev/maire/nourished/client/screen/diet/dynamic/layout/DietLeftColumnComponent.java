package dev.maire.nourished.client.screen;

import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.layout.VerticalLayout;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Today header and calorie/balance summary boxes stay drawn directly (small, non-independent
 * static text/bars not worth their own class), while "Recent Meals" and "Eat more of..." are their
 * own {@link MarieComponent}s — {@link RecentMealsComponent} and {@link EatMoreComponent}. These
 * two are no longer positioned by {@link #layout()} — a {@link Layout} recomputes child position
 * every {@code render()} call, which would silently override any future drag/resize commit on the
 * very next frame. Instead each resolves its own {@link Bounds} once at construction (an offset
 * from the panel's current position if the user has already committed a drag/resize, otherwise
 * today's default stacked position — see {@link DietScreenPersistence#resolveRelativeToPanel}),
 * and this container renders them directly against that Bounds. {@code layout()}/{@code
 * columnLayout} are kept only for {@link Container} structural
 * conformance ({@code children()}/{@code addChild()} etc.), not because anything still calls
 * {@code computeBounds()} on them. All coordinates for the header block that stays inline here are
 * expressed in DietScreen's original local (pre-scale) pixel space and converted to absolute screen
 * pixels via {@link DietLayout}'s {@code toScreenX}/{@code toScreenY}/{@code toScreenDim} helpers,
 * same as {@link DietPanelContainer}.
 */
final class DietLeftColumnComponent implements Container {

    private static final int COL_ROW_BG_RGB = 0x001E1E1E;
    private static final int COL_BORDER_LT = 0xFF555555;
    private static final int COL_SEG_EMPTY = 0xFF2A2A2A;
    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_GOLD = 0xFFFFD65C;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_ORANGE = 0xFFFFAA00;
    private static final int COL_RED = 0xFFFF5555;

    // Calories/Balance boxes aren't separate MarieComponents (no independent Bounds/DraggableResizable
    // of their own — see class javadoc), so they can't literally implement HeaderCollapsibleComponent
    // (that contract assumes ONE header + ONE body for a single component; these are two independent
    // header+body pairs drawn inline by this same container). These constants + the fit-checks below
    // replicate that interface's exact fit-check formula (header must always fit to draw anything;
    // body only draws if the box's full natural footprint also fits) against the live panel height,
    // so both boxes shrink/collapse in step with panel resize the same way the sub-boxes do, instead
    // of always drawing at a fixed size regardless of how little live room remains.
    private static final int CALORIES_HEADER_LOCAL_HEIGHT = 20;
    private static final int CALORIES_BODY_LOCAL_HEIGHT = 20;
    private static final int BALANCE_HEADER_LOCAL_HEIGHT = 30;
    private static final int BALANCE_BODY_LOCAL_HEIGHT = 20;

    private final TrackingData data;
    private final DietLayout.Layout layout;
    private final int width;
    private final int height;
    private final int headerEndLocalY;
    private final List<MarieComponent> children;
    private final Layout columnLayout;
    private Bounds recentMealsRenderBounds;
    private Bounds eatMoreRenderBounds;
    private Bounds activeEffectsRenderBounds;

    DietLeftColumnComponent(TrackingData data, DietLayout.Layout layout, int width, int height) {
        this.data = data;
        this.layout = layout;
        this.width = width;
        this.height = height;
        this.headerEndLocalY = computeHeaderEndLocalY();

        RecentMealsComponent recentMeals = new RecentMealsComponent(layout, headerEndLocalY);
        int eatMoreStartLocalY = nextSiblingStartLocalY(
                headerEndLocalY, recentMeals.localHeight(), recentMeals.resolvedBounds(), layout);
        EatMoreComponent eatMore = new EatMoreComponent(layout, eatMoreStartLocalY);
        int activeEffectsStartLocalY = nextSiblingStartLocalY(
                eatMoreStartLocalY, eatMore.localHeight(), eatMore.resolvedBounds(), layout);
        ActiveEffectsComponent activeEffects = new ActiveEffectsComponent(layout, activeEffectsStartLocalY);
        this.children = new ArrayList<>(List.of(recentMeals, eatMore, activeEffects));
        this.columnLayout = new VerticalLayout(0);
    }

    RecentMealsComponent recentMealsComponent() {
        return (RecentMealsComponent) children.get(0);
    }

    EatMoreComponent eatMoreComponent() {
        return (EatMoreComponent) children.get(1);
    }

    ActiveEffectsComponent activeEffectsComponent() {
        return (ActiveEffectsComponent) children.get(2);
    }

    /**
     * Overrides the bounds {@link #render} passes to the recent-meals/eat-more/active-effects
     * children instead of their own {@code resolvedBounds()} — for edit mode's live drag/resize
     * preview, so the single instance built here can be rendered at a live-tracked position without
     * a second, independently constructed copy. {@code null} for any param means "use that child's
     * own resolvedBounds()."
     */
    void setSubBoxRenderBounds(Bounds recentMealsBounds, Bounds eatMoreBounds, Bounds activeEffectsBounds) {
        this.recentMealsRenderBounds = recentMealsBounds;
        this.eatMoreRenderBounds = eatMoreBounds;
        this.activeEffectsRenderBounds = activeEffectsBounds;
    }

    /**
     * Local (pre-scale) Y just past the today/calorie/balance header block — matches the original
     * cursor. Package-private (not private) so {@link DietScreenEditTarget} can derive the same
     * recent-meals/eat-more start position for its edit-mode overlay without duplicating this logic.
     */
    static int computeHeaderEndLocalY() {
        NourishedClientConfig cc = NourishedClientConfig.get();
        int y = 20 + 10;
        if (FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox()) {
            y += 45;
        }
        if (cc.showBalanceBox()) {
            y += 55;
        }
        return y;
    }

    /**
     * Where the next stacked element should start, in local (pre-scale) units — advances by
     * {@code sibling}'s actual <em>footprint height</em> ({@code max(localHeight, resolvedBounds's
     * height converted to local units)}), not its content-only {@code localHeight()} alone. A box's
     * rendered height can now diverge from its content height once independently resized/persisted
     * (drag/resize, {@link DietScreenPersistence}), and the old {@code currentLocalY + localHeight}
     * formula didn't know that — so a resized-taller RecentMeals box wouldn't push EatMore/Active
     * Effects down, and they'd render overlapped by it.
     *
     * <p>Deliberately uses only {@code resolvedBounds.height()} — never {@code resolvedBounds.y()}
     * or {@code .x()} — so a box that's been <em>dragged</em> elsewhere (position changed, height
     * unchanged) doesn't drag its sibling's stacked position along with it; only an actual resize
     * (height genuinely larger than the natural content height) advances the next element. Using the
     * box's absolute Y previously coupled Active Effects' position to EatMore being moved, and fed a
     * position-inflated value back into EatMore's own {@code startLocalY} (used for its {@code
     * visible}/constraint-preferred-size computation), which could go degenerate whenever RecentMeals
     * had simply been dragged rather than resized.
     *
     * <p>Returns {@code currentLocalY} unchanged when {@code localHeight <= 0} (sibling hidden/not
     * fitting), matching the old formula's no-op in that case.
     */
    static int nextSiblingStartLocalY(int currentLocalY, int localHeight, Bounds resolvedBounds, DietLayout.Layout layout) {
        if (localHeight <= 0) {
            return currentLocalY;
        }
        int localBoundsHeight = (int) Math.round(resolvedBounds.height() / layout.scale());
        int footprint = Math.max(localHeight, localBoundsHeight);
        return currentLocalY + footprint;
    }

    @Override
    public String id() {
        return "nourished.diet.panel.left";
    }

    @Override
    public List<MarieComponent> children() {
        return children;
    }

    @Override
    public void addChild(MarieComponent child) {
        children.add(child);
    }

    @Override
    public void removeChild(MarieComponent child) {
        children.remove(child);
    }

    @Override
    public Layout layout() {
        return columnLayout;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fixed(width, height);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        if (data == null) {
            return;
        }
        NourishedClientConfig cc = NourishedClientConfig.get();
        Font font = Minecraft.getInstance().font;
        float scale = (float) layout.scale();

        int x = DietLayout.PAD;
        int y = 20;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;

        // Live-height-aware, same technique as RecentMealsComponent's constructor: layout.panelH()
        // already reflects the panel's live (possibly independently edge-resized) height, so dividing
        // by layout.scale() recovers it in the same local-unit space `y` is already expressed in.
        int liveLocalHeight = (int) Math.round(layout.panelH() / layout.scale());
        int maxY = liveLocalHeight - DietLayout.PAD;

        String todayText = Component.translatable("nourished.screen.diet.today").getString();
        int todayW = font.width(todayText);
        int todayGroupW = 16 + 4 + todayW;
        int todayStartX = x + (bw - todayGroupW) / 2;
        drawItem(context, "minecraft:sunflower", todayStartX, y - 8, scale);
        drawText(context, todayText, todayStartX + 20, y - 4, COL_GOLD, scale);
        y += 10;

        if (FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox()) {
            if (y + CALORIES_HEADER_LOCAL_HEIGHT <= maxY) {
                boolean bodyFits = y + CALORIES_HEADER_LOCAL_HEIGHT + CALORIES_BODY_LOCAL_HEIGHT <= maxY;
                drawRoundedBox(context, x - 2, y - 2, bw + 4, 40, cc);

                drawItem(context, "minecraft:fire_charge", x, y + 3, scale);
                drawText(context, Component.translatable("nourished.screen.diet.calories_label").getString(), x + 22, y + 4, COL_WHITE, scale);

                String calStr = (int) data.total + " / " + (int) data.maxTotal;
                drawText(context, calStr, x + 22, y + 15, COL_GREEN, scale);

                if (bodyFits) {
                    float calPct = data.maxTotal > 0 ? Mth.clamp(data.total / data.maxTotal, 0f, 1f) : 0f;
                    context.drawBar(sx(x), sy(y + 31), sd(bw), sd(4), calPct, COL_SEG_EMPTY, COL_GREEN);
                }
            }
            y += 45;
        }

        if (cc.showBalanceBox()) {
            if (y + BALANCE_HEADER_LOCAL_HEIGHT <= maxY) {
                boolean bodyFits = y + BALANCE_HEADER_LOCAL_HEIGHT + BALANCE_BODY_LOCAL_HEIGHT <= maxY;
                drawRoundedBox(context, x - 2, y - 2, bw + 4, 50, cc);

                drawItem(context, "minecraft:comparator", x, y + 3, scale);
                drawText(context, Component.translatable("nourished.screen.diet.balance_label").getString(), x + 22, y + 4, COL_WHITE, scale);

                String balKey = getBalanceKey(data);
                int balColor = balanceColor(balKey);
                String balText = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();

                float balanceScale = 1.2f * (10f / 9f);
                float balTextW = font.width(balText) * balanceScale;
                int bgAlpha = 51;
                int bgColor = (bgAlpha << 24) | (balColor & 0x00FFFFFF);
                context.fillRect(sx(x + 22 - 2), sy(y + 14), sd((int) balTextW + 5), sd(11), bgColor);
                drawText(context, balText, x + 22, y + 15, balColor, scale * balanceScale);

                if (bodyFits) {
                    float balScore = MarieClientCache.getBalanceScore();
                    int filledPips = Math.round(balScore * 5);
                    int pipTotalW = 5 * 10 + 4 * 3;
                    int pipStartX = x + (bw - pipTotalW) / 2;
                    for (int i = 0; i < 5; i++) {
                        int px = pipStartX + i * 13;
                        context.fillRect(sx(px), sy(y + 38), sd(10), sd(6), i < filledPips ? balColor : COL_SEG_EMPTY);
                    }
                }
            }
            y += 55;
        }
        // y is now headerEndLocalY.

        RecentMealsComponent recentMeals = (RecentMealsComponent) children.get(0);
        EatMoreComponent eatMore = (EatMoreComponent) children.get(1);
        ActiveEffectsComponent activeEffects = (ActiveEffectsComponent) children.get(2);

        Bounds recentMealsBounds = recentMealsRenderBounds != null ? recentMealsRenderBounds : recentMeals.resolvedBounds();
        Bounds eatMoreBounds = eatMoreRenderBounds != null ? eatMoreRenderBounds : eatMore.resolvedBounds();
        Bounds activeEffectsBounds = activeEffectsRenderBounds != null ? activeEffectsRenderBounds : activeEffects.resolvedBounds();

        if (recentMeals.visibilityRule().isVisible()) {
            recentMeals.render(context, recentMealsBounds);
        }
        if (eatMore.visibilityRule().isVisible()) {
            eatMore.render(context, eatMoreBounds);
        }
        if (activeEffects.visibilityRule().isVisible()) {
            activeEffects.render(context, activeEffectsBounds);
        }
    }

    // ── Coordinate + drawing helpers ─────────────────────────────────────────

    private int sx(int localX) {
        return DietLayout.toScreenX(layout, localX);
    }

    private int sy(int localY) {
        return DietLayout.toScreenY(layout, localY);
    }

    private int sd(int localDim) {
        return DietLayout.toScreenDim(layout, localDim);
    }

    private void drawText(RenderContext context, String text, int localX, int localY, int color, float scale) {
        context.drawText(text, sx(localX), sy(localY), color, scale);
    }

    private void drawItem(RenderContext context, String itemId, int localX, int localY, float scale) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(net.minecraft.world.item.Items.APPLE);
        context.drawItem(new ItemStack(item), sx(localX), sy(localY), scale);
    }

    private void drawRoundedBox(RenderContext context, int localX, int localY, int localW, int localH, NourishedClientConfig cc) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, cc.dietBackgroundOpacity());
        context.drawRoundedRect(sx(localX), sy(localY), sd(localW), sd(localH), 1, fill, COL_BORDER_LT);
    }

    private static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static String getBalanceKey(TrackingData data) {
        dev.maire.nourished.config.NourishedConfig config = dev.maire.nourished.config.NourishedConfig.get();
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
}
