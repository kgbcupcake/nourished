package dev.maire.nourished.client.screen;

import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.Constraint;
import dev.marie.framework.ui.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.layout.HorizontalLayout;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MarieUI consumer for the Diet Screen panel as a whole. Title, divider, and two column leaves
 * (left summary column, right nutrient-intake column) have every draw call's local
 * (pre-PoseStack-scale) coordinate converted to an absolute screen pixel via
 * {@link DietLayout#toScreenX}/{@link DietLayout#toScreenY}/{@link DietLayout#toScreenDim} instead
 * of relying on a global PoseStack transform, mirroring how the HUD pass converted
 * {@code HudLayout}'s pre-scaled ints into {@code NutrientBarComponent} draw calls.
 *
 * <p>The main panel's own drag/resize is handled by {@link DietScreenEditTarget}; the
 * independently-resizable recent-meals and eat-more sub-boxes are flattened into the two column
 * leaves here rather than becoming their own draggable/resizable {@link MarieComponent}s.
 */
final class DietPanelContainer implements Container {

    private static final int COL_BG_RGB = 0x00141414;
    private static final int COL_BORDER = 0xFF3A3A3A;
    private static final int COL_DIVIDER = 0xFF2E2E2E;
    private static final int COL_TITLE = 0xFF9BD36A;
    private static final int COL_GRAY = 0xFF666666;

    private final DietLayout.Layout layout;
    private final TrackingData data;
    private final List<MarieComponent> children;
    private final Layout columnLayout;

    DietPanelContainer(TrackingData data, List<String> bars, Map<String, Float> displayValues, DietLayout.Layout layout) {
        this.layout = layout;
        this.data = data;

        int gap = DietLayout.toScreenDim(layout, 1);
        int leftWidth = DietLayout.toScreenDim(layout, DietLayout.SPLIT);
        int rightWidth = Math.max(0, layout.panelW() - leftWidth - gap);
        int columnHeight = layout.panelH();

        DietLeftColumnComponent left = new DietLeftColumnComponent(data, layout, leftWidth, columnHeight);
        DietRightColumnComponent right = new DietRightColumnComponent(data, bars, displayValues, layout, rightWidth, columnHeight);
        this.children = new ArrayList<>(List.of(left, right));
        this.columnLayout = new HorizontalLayout(gap);
    }

    @Override
    public String id() {
        return "nourished.diet.panel";
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
        return Constraint.fixed(layout.panelW(), layout.panelH());
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        double opacity = NourishedClientConfig.get().dietBackgroundOpacity();
        int alpha = Math.max(0, Math.min(255, (int) Math.round(opacity * 255.0d)));
        int fill = (alpha << 24) | (COL_BG_RGB & 0x00FFFFFF);
        context.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill);
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, COL_BORDER);

        float scale = (float) layout.scale();
        Font font = Minecraft.getInstance().font;
        String title = "☘ Diet ☘";
        int titleW = (int) Math.ceil(font.width(title) * scale);
        int titleX = bounds.x() + (bounds.width() - titleW) / 2;
        context.drawText(title, titleX, DietLayout.toScreenY(layout, 9), COL_TITLE, scale);

        int dividerX = bounds.x() + DietLayout.toScreenDim(layout, DietLayout.SPLIT);
        int dividerTop = DietLayout.toScreenY(layout, 26);
        int dividerBottom = DietLayout.toScreenY(layout, DietLayout.HEIGHT - 34);
        context.fillRect(dividerX, dividerTop, DietLayout.toScreenDim(layout, 1), Math.max(0, dividerBottom - dividerTop), COL_DIVIDER);

        if (data == null) {
            String noPlayer = Component.translatable("nourished.screen.diet.no_player").getString();
            int textW = (int) Math.ceil(font.width(noPlayer) * scale);
            int textX = bounds.x() + (bounds.width() - textW) / 2;
            int textY = DietLayout.toScreenY(layout, DietLayout.HEIGHT / 2);
            context.drawText(noPlayer, textX, textY, COL_GRAY, scale);
            return;
        }

        Container.super.render(context, bounds);
    }
}
