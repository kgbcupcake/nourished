package dev.maire.nourished.client.config;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigTabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Repositions Cloth category tabs into a left sidebar and routes clicks through a dedicated widget.
 */
final class NourishedConfigLeftCardsLayout {

    private static final int NAV_LEFT = 10;
    private static final int NAV_TOP = 38;
    private static final int NAV_WIDTH = 120;
    private static final int NAV_WIDTH_MIN = 84;
    private static final int NAV_ITEM_H = 20;
    private static final int NAV_ITEM_H_FLOOR = 14;
    private static final int NAV_GAP = 4;
    private static final int NAV_GAP_FLOOR = 2;
    private static final int NAV_BOTTOM_PAD = 10;
    private static final float NAV_SCALE_MIN = 0.6f;
    private static final int WIDTH_BREAKPOINT_MAX = 640;
    private static final int WIDTH_BREAKPOINT_MIN = 480;
    private static final int CONTENT_GAP = 8;
    private static final int CONTENT_RIGHT_PAD = 12;
    private static final int CONTENT_MIN_WIDTH = 320;
    private static final int COL_SELECTED = 0xFF3A6EA5;
    private static final int COL_UNSELECTED = 0xFF1A1A1A;
    private static final int COL_UNSELECTED_BORDER = 0xFF333333;
    private static final int COL_TEXT = 0xFFE0E0E0;
    private static final int COL_SCROLLBAR = 0xFF808080;

    private NourishedConfigLeftCardsLayout() {}

    static void apply(Screen screen) {
        if (!(screen instanceof ClothConfigScreen cloth)) {
            return;
        }
        layoutLeftCards(cloth);
    }

    private static void layoutLeftCards(ClothConfigScreen cloth) {
        List<ClothConfigTabButton> tabs = getField(cloth, "tabButtons");
        if (tabs == null || tabs.isEmpty()) {
            return;
        }

        LayoutMetrics metrics = computeMetrics(cloth.width, cloth.height, tabs.size());
        applyTabLayout(tabs, metrics);

        AbstractWidget left = getField(cloth, "buttonLeftTab");
        AbstractWidget right = getField(cloth, "buttonRightTab");
        hideWidget(left);
        hideWidget(right);
        ensureSidebarWidget(cloth, tabs, metrics);
        ensureLayoutKeeper(cloth, tabs);

        applyContentLayout(cloth, metrics);
        applySelectedTabTitle(cloth);
    }

    private static void applySelectedTabTitle(ClothConfigScreen cloth) {
        Component selected = cloth.getSelectedCategory();
        if (selected != null) {
            setField(cloth, "title", selected);
        }
    }

    private static void ensureLayoutKeeper(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs) {
        List<Renderable> renderables = getField(cloth, "renderables");
        if (renderables == null) {
            return;
        }
        for (Renderable renderable : renderables) {
            if (renderable instanceof LayoutKeeperWidget) {
                return;
            }
        }
        LayoutKeeperWidget keeper = new LayoutKeeperWidget(cloth, tabs);
        renderables.add(0, keeper);
    }

    private static void hideWidget(AbstractWidget widget) {
        if (widget == null) {
            return;
        }
        widget.visible = false;
        widget.active = false;
        widget.setX(-2000);
        widget.setY(-2000);
    }

    private static void ensureSidebarWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, LayoutMetrics metrics) {
        List<Renderable> renderables = getField(cloth, "renderables");
        if (renderables == null) {
            return;
        }
        for (Renderable renderable : renderables) {
            if (renderable instanceof SidebarNavWidget sidebar) {
                sidebar.updateMetrics(metrics);
                return;
            }
        }
        SidebarNavWidget sidebar = new SidebarNavWidget(cloth, tabs, metrics);
        renderables.add(sidebar);
        List<GuiEventListener> children = getField(cloth, "children");
        if (children != null) {
            children.add(sidebar);
        }
        List<NarratableEntry> narratables = getField(cloth, "narratables");
        if (narratables != null) {
            narratables.add(sidebar);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private static LayoutMetrics computeMetrics(int screenWidth, int screenHeight, int tabCount) {
        int navWidth = interpolateNavWidth(screenWidth);

        int availableHeight = Math.max(NAV_ITEM_H_FLOOR, screenHeight - NAV_TOP - NAV_BOTTOM_PAD);
        int requiredAtDesignSize = tabCount <= 0 ? 0 : tabCount * NAV_ITEM_H + (tabCount - 1) * NAV_GAP;
        float verticalScale = requiredAtDesignSize <= 0
                ? 1f
                : clamp((float) availableHeight / requiredAtDesignSize, NAV_SCALE_MIN, 1f);

        int navItemHeight = Math.max(NAV_ITEM_H_FLOOR, Math.round(NAV_ITEM_H * verticalScale));
        int navGap = Math.max(NAV_GAP_FLOOR, Math.round(NAV_GAP * verticalScale));

        int contentLeft = NAV_LEFT + navWidth + CONTENT_GAP;
        int contentWidth = Math.max(CONTENT_MIN_WIDTH, screenWidth - contentLeft - CONTENT_RIGHT_PAD);
        return new LayoutMetrics(NAV_LEFT, NAV_TOP, navWidth, navItemHeight, navGap, contentLeft, contentWidth, availableHeight);
    }

    private static int interpolateNavWidth(int screenWidth) {
        if (screenWidth >= WIDTH_BREAKPOINT_MAX) {
            return NAV_WIDTH;
        }
        if (screenWidth <= WIDTH_BREAKPOINT_MIN) {
            return NAV_WIDTH_MIN;
        }
        float t = (float) (screenWidth - WIDTH_BREAKPOINT_MIN) / (WIDTH_BREAKPOINT_MAX - WIDTH_BREAKPOINT_MIN);
        return Math.round(NAV_WIDTH_MIN + t * (NAV_WIDTH - NAV_WIDTH_MIN));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void applyTabLayout(List<ClothConfigTabButton> tabs, LayoutMetrics metrics) {
        int y = metrics.navTop();
        for (ClothConfigTabButton tab : tabs) {
            if (tab == null) {
                continue;
            }
            tab.setX(metrics.navLeft());
            tab.setY(y);
            tab.setWidth(metrics.navWidth());
            tab.visible = false;
            y += metrics.navItemHeight() + metrics.navGap();
        }
    }

    private static void applyContentLayout(ClothConfigScreen cloth, LayoutMetrics metrics) {
        if (cloth.listWidget == null) {
            return;
        }
        cloth.listWidget.updateSize(metrics.contentWidth(), cloth.height, cloth.listWidget.top, cloth.listWidget.bottom);
        cloth.listWidget.setLeftPos(metrics.contentLeft());
    }

    private record LayoutMetrics(
            int navLeft,
            int navTop,
            int navWidth,
            int navItemHeight,
            int navGap,
            int contentLeft,
            int contentWidth,
            int navViewportHeight
    ) {}

    private static final class LayoutKeeperWidget implements Renderable {
        private final ClothConfigScreen cloth;
        private final List<ClothConfigTabButton> tabs;

        private LayoutKeeperWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs) {
            this.cloth = cloth;
            this.tabs = tabs;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            LayoutMetrics liveMetrics = computeMetrics(cloth.width, cloth.height, tabs.size());
            applyTabLayout(tabs, liveMetrics);
            applyContentLayout(cloth, liveMetrics);
            applySelectedTabTitle(cloth);
        }
    }

    private static final class SidebarNavWidget extends AbstractWidget {
        private final ClothConfigScreen cloth;
        private final List<ClothConfigTabButton> tabs;
        private LayoutMetrics metrics;
        private int scrollOffset = 0;

        private SidebarNavWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, LayoutMetrics metrics) {
            super(metrics.navLeft(), metrics.navTop(), metrics.navWidth(), metrics.navViewportHeight(), Component.empty());
            this.cloth = cloth;
            this.tabs = tabs;
            this.metrics = metrics;
        }

        private void updateMetrics(LayoutMetrics metrics) {
            this.metrics = metrics;
            setX(metrics.navLeft());
            setY(metrics.navTop());
            setWidth(metrics.navWidth());
            setHeight(metrics.navViewportHeight());
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
        }

        private int contentHeight() {
            int count = tabs.size();
            return count <= 0 ? 0 : count * metrics.navItemHeight() + (count - 1) * metrics.navGap();
        }

        private int maxScroll() {
            return Math.max(0, contentHeight() - metrics.navViewportHeight());
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            updateDynamicLayout();
            int idx = indexAt(mouseX, mouseY);
            if (idx < 0 || idx >= tabs.size()) {
                return;
            }
            ClothConfigTabButton tab = tabs.get(idx);
            if (tab != null) {
                tab.onPress();
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (maxScroll() <= 0 || !isMouseOver(mouseX, mouseY)) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (scrollY * metrics.navItemHeight()), maxScroll()));
            return true;
        }

        @Override
        protected void updateWidgetNarration(@Nonnull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {}

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            updateDynamicLayout();
            int viewportBottom = metrics.navTop() + metrics.navViewportHeight();
            var font = Objects.requireNonNull(Minecraft.getInstance().font);

            graphics.enableScissor(metrics.navLeft(), metrics.navTop(), metrics.navLeft() + metrics.navWidth(), viewportBottom);
            int y = metrics.navTop() - scrollOffset;
            for (ClothConfigTabButton tab : tabs) {
                if (tab == null) {
                    y += metrics.navItemHeight() + metrics.navGap();
                    continue;
                }
                if (y + metrics.navItemHeight() >= metrics.navTop() && y <= viewportBottom) {
                    boolean selected = !tab.active;
                    int fill = selected ? COL_SELECTED : COL_UNSELECTED;
                    graphics.fill(metrics.navLeft(), y, metrics.navLeft() + metrics.navWidth(), y + metrics.navItemHeight(), fill);
                    graphics.renderOutline(metrics.navLeft(), y, metrics.navWidth(), metrics.navItemHeight(), COL_UNSELECTED_BORDER);
                    Component label = Objects.requireNonNull(tab.getMessage());
                    int maxTextWidth = metrics.navWidth() - 12;
                    String text = Objects.requireNonNull(font.plainSubstrByWidth(Objects.requireNonNull(label.getString()), maxTextWidth));
                    int textY = y + (metrics.navItemHeight() - font.lineHeight) / 2;
                    graphics.drawString(font, text, metrics.navLeft() + 6, textY, COL_TEXT, false);
                }
                y += metrics.navItemHeight() + metrics.navGap();
            }
            graphics.disableScissor();

            int maxScroll = maxScroll();
            if (maxScroll > 0) {
                int barX = metrics.navLeft() + metrics.navWidth() - 2;
                int trackHeight = metrics.navViewportHeight();
                int barHeight = Math.max(10, trackHeight * trackHeight / contentHeight());
                int barY = metrics.navTop() + (trackHeight - barHeight) * scrollOffset / maxScroll;
                graphics.fill(barX, barY, barX + 2, barY + barHeight, COL_SCROLLBAR);
            }
        }

        private int indexAt(double mouseX, double mouseY) {
            if (mouseX < metrics.navLeft() || mouseX > metrics.navLeft() + metrics.navWidth()) {
                return -1;
            }
            if (mouseY < metrics.navTop() || mouseY > metrics.navTop() + metrics.navViewportHeight()) {
                return -1;
            }
            int y = metrics.navTop() - scrollOffset;
            for (int i = 0; i < tabs.size(); i++) {
                if (mouseY >= y && mouseY < y + metrics.navItemHeight()) {
                    return i;
                }
                y += metrics.navItemHeight() + metrics.navGap();
            }
            return -1;
        }

        private void updateDynamicLayout() {
            LayoutMetrics liveMetrics = computeMetrics(cloth.width, cloth.height, tabs.size());
            updateMetrics(liveMetrics);
            applyTabLayout(tabs, liveMetrics);
            applyContentLayout(cloth, liveMetrics);
        }
    }
}
