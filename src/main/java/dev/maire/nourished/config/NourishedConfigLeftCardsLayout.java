package dev.maire.nourished.config;

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
 * Dedicated layout controller for the config navigator.
 *
 * Keeps the existing Cloth categories/entries intact and only repositions
 * category tab widgets into a left-side card/box column.
 */
final class NourishedConfigLeftCardsLayout {
    private static final int NAV_LEFT = 10;
    private static final int NAV_TOP = 38;
    private static final int NAV_WIDTH = 120;
    private static final int NAV_WIDTH_MIN = 84;
    private static final int NAV_ITEM_H = 20;
    private static final int NAV_GAP = 4;
    private static final int CONTENT_GAP = 8;
    private static final int CONTENT_RIGHT_PAD = 12;
    private static final int CONTENT_MIN_WIDTH = 520;
    private static final int COL_SELECTED = 0xFF3A6EA5;
    private static final int COL_UNSELECTED = 0xFF1A1A1A;
    private static final int COL_UNSELECTED_BORDER = 0xFF333333;
    private static final int COL_TEXT = 0xFFE0E0E0;

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

        LayoutMetrics metrics = computeMetrics(cloth.width);
        applyTabLayout(tabs, metrics);

        AbstractWidget left = getField(cloth, "buttonLeftTab");
        AbstractWidget right = getField(cloth, "buttonRightTab");
        hideWidget(left);
        hideWidget(right);
        ensureSidebarWidget(cloth, tabs, metrics);

        applyContentLayout(cloth, metrics);
    }

    private static void hideWidget(AbstractWidget widget) {
        if (widget == null) return;
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

    private static LayoutMetrics computeMetrics(int screenWidth) {
        int availableForPanels = Math.max(200, screenWidth - NAV_LEFT - CONTENT_RIGHT_PAD);
        int desiredNavWidth = Math.min(NAV_WIDTH, Math.max(NAV_WIDTH_MIN, availableForPanels - CONTENT_MIN_WIDTH - CONTENT_GAP));
        int navWidth = Math.max(NAV_WIDTH_MIN, Math.min(NAV_WIDTH, desiredNavWidth));
        int contentLeft = NAV_LEFT + navWidth + CONTENT_GAP;
        int contentWidth = Math.max(120, screenWidth - contentLeft - CONTENT_RIGHT_PAD);
        return new LayoutMetrics(NAV_LEFT, NAV_TOP, navWidth, NAV_ITEM_H, NAV_GAP, contentLeft, contentWidth);
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
            int contentWidth
    ) {}

    private static final class SidebarNavWidget extends AbstractWidget {
        private final ClothConfigScreen cloth;
        private final List<ClothConfigTabButton> tabs;
        private LayoutMetrics metrics;

        private SidebarNavWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, LayoutMetrics metrics) {
            super(metrics.navLeft(), metrics.navTop(), metrics.navWidth(), 1, Component.empty());
            this.cloth = cloth;
            this.tabs = tabs;
            this.metrics = metrics;
        }

        private void updateMetrics(LayoutMetrics metrics) {
            this.metrics = metrics;
            setX(metrics.navLeft());
            setY(metrics.navTop());
            setWidth(metrics.navWidth());
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
        protected void updateWidgetNarration(@Nonnull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {}

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            updateDynamicLayout();
            int y = metrics.navTop();
            var font = Objects.requireNonNull(Minecraft.getInstance().font);
            for (ClothConfigTabButton tab : tabs) {
                if (tab == null) {
                    y += metrics.navItemHeight() + metrics.navGap();
                    continue;
                }
                boolean selected = !tab.active;
                int fill = selected ? COL_SELECTED : COL_UNSELECTED;
                int border = COL_UNSELECTED_BORDER;
                graphics.fill(metrics.navLeft(), y, metrics.navLeft() + metrics.navWidth(), y + metrics.navItemHeight(), fill);
                graphics.renderOutline(metrics.navLeft(), y, metrics.navWidth(), metrics.navItemHeight(), border);
                Component label = Objects.requireNonNull(tab.getMessage());
                int maxTextWidth = metrics.navWidth() - 12;
                String fullText = Objects.requireNonNull(label.getString());
                String text = Objects.requireNonNull(font.plainSubstrByWidth(fullText, maxTextWidth));
                int textY = y + (metrics.navItemHeight() - font.lineHeight) / 2;
                graphics.drawString(font, text, metrics.navLeft() + 6, textY, COL_TEXT, false);
                y += metrics.navItemHeight() + metrics.navGap();
            }
            setHeight(Math.max(1, y - metrics.navTop()));
        }

        private int indexAt(double mouseX, double mouseY) {
            if (mouseX < metrics.navLeft() || mouseX > metrics.navLeft() + metrics.navWidth()) {
                return -1;
            }
            int y = metrics.navTop();
            for (int i = 0; i < tabs.size(); i++) {
                if (mouseY >= y && mouseY < y + metrics.navItemHeight()) {
                    return i;
                }
                y += metrics.navItemHeight() + metrics.navGap();
            }
            return -1;
        }

        private void updateDynamicLayout() {
            LayoutMetrics liveMetrics = computeMetrics(cloth.width);
            updateMetrics(liveMetrics);
            applyTabLayout(tabs, liveMetrics);
            applyContentLayout(cloth, liveMetrics);
        }
    }
}
